package com.champaca.inventorydata.conditionroom.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.conditionroom.ConditionRoomService
import com.champaca.inventorydata.conditionroom.request.AssembleRequest
import com.champaca.inventorydata.conditionroom.response.AssembleResponse
import com.champaca.inventorydata.databasetable.LotNo
import com.champaca.inventorydata.databasetable.Pile
import com.champaca.inventorydata.databasetable.dao.LotNoDao
import com.champaca.inventorydata.databasetable.dao.PileDao
import com.champaca.inventorydata.databasetable.dao.PileTransactionDao
import com.champaca.inventorydata.databasetable.dao.PileTransactionDao.Companion.ASSEMBLE
import com.champaca.inventorydata.databasetable.dao.PileTransactionDao.Companion.PICK
import com.champaca.inventorydata.databasetable.dao.PileTransactionDao.Companion.PICK_FOR_ASSEMBLE
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.config.ConfigRepository
import com.champaca.inventorydata.masterdata.storelocation.StoreLocationRepository
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.model.reduceBySkuId
import com.champaca.inventorydata.utils.DateTimeUtil
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class AssembleUseCase(
    val dataSource: DataSource,
    val dateTimeUtil: DateTimeUtil,
    val pileService: PileService,
    val configRepository: ConfigRepository,
    val wmsService: WmsService,
    val storeLocationRepository: StoreLocationRepository,
    val conditionRoomService: ConditionRoomService,
    val itemLockService: ItemLockService
) {
    val logger = LoggerFactory.getLogger(AssembleUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    companion object {
        const val OUTGIONG_PICKING_PREFIX = "OutgoingPickingOrder"
        const val OUTGIONG_GOOD_RECEIPT_PREFIX = "OutgoingGoodReceipt"
    }

// ขั้นตอนการเบิกไม้จากห้อง Condition Room
//    1. เปิดใบเบิก
//    2. เบิกไม้จากกองต่างๆ
//      2.1. เลือกใบเบิก
//      2.2. แสกนที่กองไม้ห้องคอน  /pile/wms/lots/{pileCode}
//      2.3. ระบุ mat_code และจำนวนที่จะเบิก
//          2.3.1 เป็น check box ให้เลือก แล้วก็มี text field ให้ใส่จำนวนด้านหลัง
//      2.4. กดปุ่ม “กองถัดไป”
//          2.4.1 เก็บ pileCode, skuId, qty ไว้ในมือถือก่อน
//      2.5. ทำวนข้อ b – d จนเลือกไม้ได้ครบ 1 กอง
//      2.6. กดปุ่ม “ตั้งกอง”  /conditionRoom/wms/pick ==> Use Case นี้แหละ!!!
//          2.6.1. เบิกเข้าใบเบิกห้องคอนขาออก
//          2.6.2. สร้างรหัสกองใหม่ ใช้รหัสเป็น CX
//          2.6.3. สร้าง lotNo ใหม่ในใบรับห้องคอนขาออก
//          2.6.4. เบิก lotNo ใหม่เข้าใบเบิกที่ User เลือก
//          2.6.5. สร้าง pile ใหม่, เอา LotNo ใหม่เข้า pile, ใส่ originGoodMovementId เป็น ใบรับห้องคอนขาออก, goodMovementId เป็นใบเบิกที่ User เลือก
//          2.6.6. บันทึก pileTransaction ว่ากองใหม่เบิกจากห้องคอน (type: pickFromCondRoom)
//              1. FromLotNoIds  Lot ของกองไม้ห้องคอน
//              2. toLotNoIds  Lot ใหม่ที่สร้าง
//          2.6.7. บันทึก pileTransaction ว่ากองห้องคอนถูกเบิก (type: partialPick)


    fun execute(sessionId: String, userId: String, request: AssembleRequest): AssembleResponse {
        val palletCodes = request.pickDetails.map { it.fromPileCode }.joinToString(",")
        val lockName = "Assemble${palletCodes}"
        try {
            itemLockService.lock(lockName)
            Database.connect(dataSource)

            var errorType = PileError.NONE
            var errorMessage = ""
            var pickedItemMap: Map<String, List<MovingItem>> = mapOf()
            var remainingItemMap: Map<String, List<MovingItem>> = mapOf()
            var pickedItems: List<WmsService.ItemMovementEntry> = listOf()
            var receivedItems: List<WmsService.ItemMovementEntry> = listOf()
            var newPileCode = ""
            var outgoingPickingId = -1
            var outgoingGoodReceiptId = -1
            var storeLocationId = -1
            transaction {
                addLogger(exposedLogger)

                val verifyResult = verifyAndPickItems(request)
                if (verifyResult is ResultOf.Failure) {
                    errorMessage = verifyResult.message!!
                    errorType = PileError.LOTS_ARE_OVERPICKED
                    logger.warn(errorMessage)
                    return@transaction
                }
                (verifyResult as ResultOf.Success).value.let {
                    remainingItemMap = it.first
                    pickedItemMap = it.second
                }

                val storeLocation = storeLocationRepository.getByCode(request.location)
                if (storeLocation == null) {
                    errorType = PileError.LOCATION_NOT_FOUND
                    logger.warn("Location: ${request.location} not found")
                    return@transaction
                }
                storeLocationId = storeLocation.id.value

                val newPileResult = pileService.newPileCodeAndIncreaseRunningNumber(request.departmentPrefix)
                if (newPileResult is ResultOf.Failure) {
                    errorType = PileError.PILE_CODE_EXIST
                    errorMessage = "Pile code ${newPileResult.message} already exists"
                    logger.warn("Pile ${newPileResult.message}: Pile code already exists")
                    return@transaction
                }
                newPileCode = (newPileResult as ResultOf.Success).value

                val monthPrefix = dateTimeUtil.getYearMonthPrefix()
                val outgoingPickingConfigName = "$OUTGIONG_PICKING_PREFIX.${request.departmentPrefix}"
                val outgoingPickingResult =
                    conditionRoomService.getIntermediateGoodMovementId(outgoingPickingConfigName)
                if (outgoingPickingResult is ResultOf.Failure) {
                    errorType = PileError.INTERMEDIATE_GOOD_MOVEMENT_ID_NOT_FOUND
                    errorMessage = "Config not found for key ${outgoingPickingConfigName}.${monthPrefix}"
                    logger.warn("Config not found for key $outgoingPickingConfigName.$monthPrefix")
                    return@transaction
                }
                outgoingPickingId = (outgoingPickingResult as ResultOf.Success).value

                val outgoingGoodReceiptConfigName = "$OUTGIONG_GOOD_RECEIPT_PREFIX.${request.departmentPrefix}"
                val outgoingGoodReceiptResult =
                    conditionRoomService.getIntermediateGoodMovementId(outgoingGoodReceiptConfigName)
                if (outgoingGoodReceiptResult is ResultOf.Failure) {
                    errorType = PileError.INTERMEDIATE_GOOD_MOVEMENT_ID_NOT_FOUND
                    errorMessage = "Config not found for key ${outgoingGoodReceiptConfigName}.${monthPrefix}"
                    logger.warn("Config not found for key $outgoingGoodReceiptConfigName.$monthPrefix")
                    return@transaction
                }
                outgoingGoodReceiptId = (outgoingGoodReceiptResult as ResultOf.Success).value

                pickedItems = createPickedItemMovements(outgoingPickingId, pickedItemMap.flatMap { it.value })
                receivedItems = createReceiveItemMovements(outgoingGoodReceiptId, request, storeLocationId, newPileCode)
            }

            logger.info("Picked: ${pickedItemMap.mapValues { (pileCode, entries) -> entries.map { entry -> entry.matCode to entry.qty } }}")

            if (errorType != PileError.NONE) {
                return AssembleResponse.Failure(
                    errorType = errorType,
                    errorMessage = errorMessage
                )
            }

            val pickResult = wmsService.pickGmItem(sessionId, pickedItems)
            if (pickResult is ResultOf.Failure) {
                logger.warn("Failed to pick items, error message: $errorMessage. New pile code: $newPileCode")
                return AssembleResponse.Failure(
                    errorType = PileError.WMS_VALIDATION_ERROR,
                    errorMessage = pickResult.message
                )
            }

            val receiveResult = wmsService.receiveGmItem(sessionId, receivedItems)
            if (receiveResult is ResultOf.Failure) {
                logger.warn("Failed to receive items, error message: $errorMessage")
                return AssembleResponse.Failure(
                    errorType = PileError.WMS_VALIDATION_ERROR,
                    errorMessage = receiveResult.message
                )
            }
            logger.info("Received items successfully. New pile code: $newPileCode")
            transaction {
                addLogger(exposedLogger)
                receivedItems = fillReceivingLotNoIds(receivedItems)
                upsertPileData(
                    request = request,
                    goodMovementId = request.pickingGoodMovementId,
                    pickedItemMap = pickedItemMap,
                    remainingItemMap = remainingItemMap,
                    receivingItems = receivedItems,
                    newPileCode = newPileCode,
                    storeLocationId = storeLocationId,
                    outgoingPickingId = outgoingPickingId,
                    outgoingReceivingId = outgoingGoodReceiptId,
                    userId = userId
                )
            }

            return AssembleResponse.Success(
                pileCode = newPileCode
            )
        } finally {
            itemLockService.unlock(lockName)
        }
    }

    private fun verifyAndPickItems(request: AssembleRequest): ResultOf<Pair<Map<String, List<MovingItem>>, Map<String, List<MovingItem>>>> {
        val fromPileCodes = request.pickDetails.map { it.fromPileCode }
        val skuIds = request.pickDetails.map { it.items.map { it.skuId } }.flatten().distinct()
        val items = pileService.findItemsInStorageArea(fromPileCodes, skuIds)
        val itemByPilesBySku = items.groupBy { it.lotRefCode.substringBefore("_") }
            .mapValues { (_, entries) ->entries.groupBy { it.skuId } }

        val requiredQtyByPileBySku = request.pickDetails.associateBy( {it.fromPileCode} )
            .mapValues { (_, entries) -> entries.items.groupBy { it.skuId }.mapValues { (_, itemEntries) -> itemEntries.sumOf { it.qty } } }

        val availableItemsBySkuId = items.reduceBySkuId().associateBy { it.skuId }
        val requiredItemsBySkuId = request.pickDetails.flatMap { it.items }
            .groupBy { it.skuId }
            .mapValues { (_, entries) -> entries.sumOf { it.qty } }
        requiredItemsBySkuId.forEach { (skuId, requiredAmount) ->
            val availableAmount = availableItemsBySkuId[skuId]?.qty ?: BigDecimal.ZERO
            if (requiredAmount > availableAmount) {
                return ResultOf.Failure("Unable to pick. Required amount of SKU: $skuId is $requiredAmount, but only $availableAmount is available.")
            }
        }

        val pickedItemMap = mutableMapOf<String, List<MovingItem>>()
        val remainingItemMap = mutableMapOf<String, List<MovingItem>>()

        requiredQtyByPileBySku.forEach { (pileCode, skuQtyMap) ->
            val pickedItems = mutableListOf<MovingItem>()
            val remainingItems = mutableListOf<MovingItem>()
            skuQtyMap.forEach { (skuId, qty) ->
                var requiredQty = qty
                val availableItems = itemByPilesBySku[pileCode]!![skuId]!!
                    .sortedByDescending { it.lotNoId }  // ที่ต้อง sort ให้เอา lot หลังขึ้นมาก่อนเพราะคุยกับพี่แทน (CFO Champaca) แล้วว่า การดึงไม้ออกจากห้อง Condition เป็นแบบ LIFO
                var index = 0
                while(requiredQty > BigDecimal.ZERO && index < availableItems.size){
                    val availableItem = availableItems[index]
                    val remainingItem = availableItem.clone()
                    if(availableItem.qty <= requiredQty){
                        remainingItem.qty = BigDecimal.ZERO
                        requiredQty -= availableItem.qty
                    } else {
                        remainingItem.qty = availableItem.qty - requiredQty
                        availableItem.qty = requiredQty
                        requiredQty = BigDecimal.ZERO
                    }
                    pickedItems.add(availableItem)
                    remainingItems.add(remainingItem)
                    index++
                }
            }
            pickedItemMap[pileCode] = pickedItems
            remainingItemMap[pileCode] = remainingItems
        }

        return ResultOf.Success(Pair(remainingItemMap, pickedItemMap))
    }

    private fun createPickedItemMovements(outgoingPickingId: Int, pickedItems: List<MovingItem>): List<WmsService.ItemMovementEntry> {
        return pickedItems.map {
            WmsService.ItemMovementEntry(
                goodMovementType = GoodMovementType.PICKING_ORDER,
                goodMovementId = outgoingPickingId,
                skuId = it.skuId,
                storeLocationId = it.storeLocationId,
                manufacturingLineId = null,
                lotNoId = it.lotNoId,
                lotNo = it.lotRefCode,
                qty = it.qty,
                refCode = it.lotRefCode,
                // ใน remark ให้มีคำว่า partial pick เวลาตอนดูใบเบิกผ่าน /goodsMovement/wms/pile/{goodMovementId} จะได้รู้ว่านี่เป็น
                // การเบิกไม้บางส่วน
                remark = PileTransactionDao.PARTIAL_PICK + " from pile: ${it.pilecode}"
            )
        }
    }

    private fun createReceiveItemMovements(outgoingGoodReceiptId: Int,
                                           request: AssembleRequest,
                                           storeLocationId: Int,
                                           pileCode: String): List<WmsService.ItemMovementEntry> {
        val skuQtyMap = request.pickDetails.flatMap { it.items }
            .groupBy { it.skuId }
            .mapValues { (_, entries) -> entries.sumOf { it.qty } }
        var index = 0
        return skuQtyMap.map { (skuId, qty) ->
            index++
            WmsService.ItemMovementEntry(
                goodMovementType = GoodMovementType.GOODS_RECEIPT,
                goodMovementId = outgoingGoodReceiptId,
                skuId = skuId,
                storeLocationId = storeLocationId,
                manufacturingLineId = null,
                qty = qty,
                refCode = "${pileCode}_${request.departmentPrefix}${index}",
            )
        }
    }

    private fun fillReceivingLotNoIds(receiveItems: List<WmsService.ItemMovementEntry>): List<WmsService.ItemMovementEntry> {
        val lotNoDaos = LotNoDao.find { (LotNo.refCode inList receiveItems.map { it.refCode!! }) and (LotNo.status eq "A") }
            .toList()
            .associateBy { it.refCode }
        receiveItems.forEach { item ->
            val lotNoDao = lotNoDaos[item.refCode!!]!!
            item.apply {
                lotNo = lotNoDao.code
                lotNoId = lotNoDao.id.value
            }
        }
        return receiveItems
    }

    private fun upsertPileData(request: AssembleRequest,
                               goodMovementId: Int?,
                               pickedItemMap: Map<String, List<MovingItem>>,
                               remainingItemMap: Map<String, List<MovingItem>>,
                               receivingItems: List<WmsService.ItemMovementEntry>,
                               newPileCode: String,
                               storeLocationId: Int,
                               outgoingPickingId: Int,
                               outgoingReceivingId: Int,
                               userId: String) {
        val now = LocalDateTime.now()
        // First, create a new pile for picked items.
        val newPileId = Pile.insertAndGetId {
            it[this.manufacturingLineId] = null
            if (goodMovementId != null) {
                it[this.goodMovementId] = goodMovementId
            } else {
                it[this.goodMovementId] = outgoingReceivingId
            }
            it[this.originGoodMovementId] = outgoingReceivingId
            it[this.storeLocationId] = storeLocationId
            it[this.code] = newPileCode
            it[this.processTypePrefix] = request.departmentPrefix
            it[this.orderNo] = request.orderNo
            it[this.lotSet] = 1
            it[this.type] = PileDao.ASSEMBLED_WOOD_PILE
            it[this.remark] = ""
            it[this.createdAt] = now
            it[this.updatedAt] = now
            it[this.status] = "A"
        }
        pileService.addPileHasLotNos(newPileId.value, receivingItems.map { it.lotNoId!! }, 1)
        // Mark it as created
        pileService.addPileTransaction(
            pileId = newPileId.value,
            toGoodMovementId = outgoingReceivingId,
            userId = userId.toInt(),
            type = ASSEMBLE,
            fromPiles = pickedItemMap.keys.toList(),
            toLotNos = receivingItems.map { it.lotNoId!! },
            movingQty = receivingItems.map { it.qty },
        )

        // Then, update the 'from' piles that they have transferred items into a newly created pile going out of
        // the condition room.
        Pile.update({ Pile.code inList pickedItemMap.keys }) {
            it[Pile.updatedAt] = now
        }
        pickedItemMap.forEach { (pileCode, movingItems) ->
            val pile = PileDao.find { Pile.code eq pileCode }.first()
            pileService.addPileTransaction(
                pileId = pile.id.value,
                fromGoodMovementId = pile.goodMovementId.value,
                toGoodMovementId = outgoingPickingId,
                userId = userId.toInt(),
                type = PICK_FOR_ASSEMBLE,
                toPileId = newPileId.value,
                fromLotNos = movingItems.map { it.lotNoId },
                movingQty = movingItems.map { it.qty },
                remainingQty = remainingItemMap[pileCode]!!.map { it.qty }
            )
        }
    }
}