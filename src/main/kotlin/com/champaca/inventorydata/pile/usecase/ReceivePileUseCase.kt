package com.champaca.inventorydata.pile.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.manufacturingline.ManufacturingLineRepository
import com.champaca.inventorydata.masterdata.storelocation.StoreLocationRepository
import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.request.ReceivePileRequest
import com.champaca.inventorydata.pile.response.ReceivePileResponse
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class ReceivePileUseCase(
    val dataSource: DataSource,
    val pileService: PileService,
    val wmsService: WmsService,
    val storeLocationRepository: StoreLocationRepository,
    val itemLockService: ItemLockService,
    val manufacturingLineRepository: ManufacturingLineRepository
) {
    val logger = LoggerFactory.getLogger(ReceivePileUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, userId: String, request: ReceivePileRequest): ReceivePileResponse {
        try {
            itemLockService.lock(request.pileCode)   // Lock the pile
            Database.connect(dataSource)

            var errorType = PileError.NONE
            var errorMessage = ""
            var receivedItems: List<MovingItem> = listOf()
            var pile: PileDao? = null
            var storeLocationId = -1
            transaction {
                addLogger(exposedLogger)
                val pair = pileService.findPileAndCurrentLotNos(request.pileCode)
                if (pair == null) {
                    errorType = PileError.PILE_NOT_FOUND
                    logger.warn("Pile: ${request.pileCode} not found")
                    return@transaction
                }

                pile = pair.first
                val lotNoIds = pair.second.map { it.id.value }
                if (pile!!.goodMovementId.value == request.goodMovementId) {
                    // If the pile's latest goodMovementId equals to the request's goodMovementId, this is probably because
                    // the user scans the same pile twice. Hence, we block the second pile receive
                    errorType = PileError.PILE_HAS_BEEN_RECEIVED
                    errorMessage = pile!!.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    logger.warn("Pile: ${request.pileCode} has been received at ${errorMessage}")
                    return@transaction
                }

                val pickingGoodMovementPair = getPickingGoodMovementId(request, pile!!)
                if (pickingGoodMovementPair.second != PileError.NONE) {
                    errorType = pickingGoodMovementPair.second
                    logger.warn("Pile: ${request.pileCode} has no picking good movement")
                    return@transaction
                }
                val pickingGoodMovementIds = pickingGoodMovementPair.first

                val storeLocation = storeLocationRepository.getByCode(request.location)
                if (storeLocation == null) {
                    errorType = PileError.LOCATION_NOT_FOUND
                    logger.warn("Pile: ${request.pileCode}, Location: ${request.location} not found")
                    return@transaction
                }
                storeLocationId = storeLocation.id.value

                receivedItems = pileService.findReceivingItems(lotNoIds, pickingGoodMovementIds)
                if (receivedItems.isEmpty()) {
                    val goodMovement = getGoodReceiptGoodMovement(lotNoIds)
                    if (goodMovement != null) {
                        errorMessage = "${goodMovement.code}"
                    }
                    errorType = PileError.NO_ITEMS_TO_BE_RECEIVED
                    logger.warn("Pile: ${request.pileCode} has no items to be received")
                    return@transaction
                }

                val previousSkuGroup = receivedItems.first().skuName.substring(1, 3)
                if (previousSkuGroup != request.skuGroupCode) {
                    val result = pileService.createNewReceivingItems(receivedItems, request.skuGroupCode)
                    if (result is ResultOf.Failure) {
                        errorType = PileError.NON_EXISTING_MATCODE
                        errorMessage = result.message!!
                        logger.warn("Pile: ${request.pileCode} has non-existing matCode: ${errorMessage}")
                        return@transaction
                    }
                    receivedItems = (result as ResultOf.Success).value
                }
            }

            if (errorType != PileError.NONE) {
                return ReceivePileResponse.Failure(errorType = errorType, errorMessage = errorMessage)
            }

            val lotGroupRefCode = "${pile!!.code}_${request.processPrefix}"
            val itemMovements = createItemMovements(request, receivedItems, lotGroupRefCode, storeLocationId)
            val result = wmsService.receiveGmItem(sessionId, itemMovements)
            if (result is ResultOf.Failure) {
                logger.warn("Pile ${request.pileCode}: WMS validation error: ${result.message}")
                return ReceivePileResponse.Failure(
                    errorType = PileError.WMS_VALIDATION_ERROR,
                    errorMessage = (result as ResultOf.Failure).message
                )
            }

            var pileCount = 0
            var itemCount = 0
            transaction {
                // Record Transaction and changes
                addLogger(exposedLogger)
                val lotRefCodes = itemMovements.map { it.refCode!! }
                val lotNoIds = getLotNoIdFromRefCode(lotRefCodes)
                upsertPileRecords(userId, request, pile!!, receivedItems, lotNoIds, storeLocationId)
                pileService.getPileAndItemCount(request.goodMovementId).apply {
                    pileCount = this.first
                    itemCount = this.second
                }
            }

            return ReceivePileResponse.Success(
                receivingItems = receivedItems,
                pileCount = pileCount,
                itemCount = itemCount)
        } finally {
            itemLockService.unlock(request.pileCode)   // Unlock the pile
        }
    }

    private fun getLotNoIdFromRefCode(refCodes: List<String>): List<Int> {
        val joins = StoreLocationHasLotNo.join(LotNo, JoinType.INNER) { StoreLocationHasLotNo.lotNoId eq LotNo.id }
        val query = joins.select(LotNo.id)
            .where { (LotNo.refCode.inList(refCodes)) and (LotNo.status eq "A") and (StoreLocationHasLotNo.qty greater 0) }
        return query.map { resultRow -> resultRow[LotNo.id].value }
    }

    private fun createItemMovements(request: ReceivePileRequest, items: List<MovingItem>, lotGroupRefCode: String, storeLocationId: Int): List<WmsService.ItemMovementEntry> {
        return items.mapIndexed { index, pickingData ->
            WmsService.ItemMovementEntry(
                goodMovementType = GoodMovementType.GOODS_RECEIPT,
                goodMovementId = request.goodMovementId,
                skuId = pickingData.skuId,
                sku = pickingData.matCode,
                storeLocationId = storeLocationId,
                storeLocation = request.location,
                manufacturingLineId = request.manufacturingLineId,
                qty = pickingData.qty.setScale(2, RoundingMode.HALF_UP),
                refCode = "${lotGroupRefCode}${(index + 1).toString().padStart(2, '0')}"
            )
        }
    }

    private fun upsertPileRecords(userId: String, request: ReceivePileRequest, pile: PileDao,
                                  receivedItems: List<MovingItem>, lotNoIds: List<Int>, storeLocationId: Int) {
        val manufacturingLine = manufacturingLineRepository.findById(request.manufacturingLineId)
        val newLotSet = pile.lotSet + 1

        // Update pile with the new good movement and lot set
        Pile.update({Pile.id eq pile.id.value}) {
            it[this.storeLocationId] = storeLocationId
            it[this.goodMovementId] = request.goodMovementId
            it[this.lotSet] = newLotSet
            it[this.updatedAt] = LocalDateTime.now()

            if (manufacturingLine != null) {
                val extraAttributes = pile.extraAttributes?.toMutableMap() ?: mutableMapOf()
                extraAttributes[request.processPrefix] = manufacturingLine.name
                it[this.extraAttributes] = extraAttributes
            }
        }

        // Bind pile with a new set of lotNos
        pileService.addPileHasLotNos(pile.id.value, lotNoIds, newLotSet)

        // Record pile receive from the process
        pileService.addPileTransaction(pileId = pile.id.value,
            fromGoodMovementId = pile.goodMovementId.value,
            toGoodMovementId = request.goodMovementId,
            userId = userId.toInt(),
            type = PileTransactionDao.RECEIVE,
            fromLotNos = receivedItems.map { item -> item.lotNoId },
            toLotNos = lotNoIds,
            remainingQty = receivedItems.map { item -> item.qty },
        )

    }

    private fun getPickingGoodMovementId(request: ReceivePileRequest, pile: PileDao): Pair<List<Int>, PileError> {
        if (request.receiveType == ReceivePileRequest.PROCESS) {
            // ถ้าเป็นการรับจาก process ให้หา good movement ที่เป็น good receipt ของ good movement ปัจจุบัน
            val pickingGoodMovements  = GoodMovementDao.find { GoodMovement.goodReceiptGoodMovementId eq request.goodMovementId }.toList()
            if (pickingGoodMovements.isEmpty()) {
                // this is just to make sure that Data team link the good receipt with the picking order otherwise
                // the stock will stuck in manufacturing_line_has_lot_no
                return Pair(listOf(), PileError.NO_GOOD_RECEIPT_LINKS)
            }
            return Pair(listOf(pile.goodMovementId.value), PileError.NONE)
        } else {
            // ถ้าเป็นการรับจาก transfer ให้ดูว่า good movement ปลายทางมี transferGoodMovementId มั้ย
            val goodMovement = GoodMovementDao.findById(request.goodMovementId)!!
            if (goodMovement.transferGoodMovementId == null) {
                return Pair(listOf(), PileError.NO_TRANSFER_GOOD_MOVEMENT)
            }
            return Pair(listOf(goodMovement.transferGoodMovementId!!.value), PileError.NONE)
        }
    }

    /**
     * This function is used in the case of error when we can't find any item to be received, i.e. pickingItems list is empty.
     * In such case we will try to inform the user of which good receipt that the items are likely located at.
     */
    private fun getGoodReceiptGoodMovement(lotNoIds: List<Int>): GoodMovementDao? {
        val joins = GmItem.join(GoodMovement, JoinType.INNER) { (GmItem.goodMovementId eq GoodMovement.id) and (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) }
            .join(LotNo, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
        val query = joins.select(GoodMovement.columns)
            .where { (LotNo.status eq "A") and (GmItem.status eq "A") and (GoodMovement.status eq "A") and
                    (LotNo.id inList lotNoIds) }
        val results = GoodMovementDao.wrapRows(query).toList()
        return results.firstOrNull()
    }
}