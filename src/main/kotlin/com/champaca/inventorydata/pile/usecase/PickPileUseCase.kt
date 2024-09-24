package com.champaca.inventorydata.pile.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.Pile
import com.champaca.inventorydata.databasetable.dao.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.storelocation.StoreLocationRepository
import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.model.PickedGoodMovement
import com.champaca.inventorydata.pile.request.PickPileRequest
import com.champaca.inventorydata.pile.response.PickPileResponse
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
class PickPileUseCase(
    val dataSource: DataSource,
    val pileService: PileService,
    val wmsService: WmsService,
    val itemLockService: ItemLockService,
    val storeLocationRepository: StoreLocationRepository
) {

    companion object {
        const val ACQ_DEPARTMENT_ID = 2
        const val KILN_DRY_DEPARTMENT_ID = 3
    }

    val logger = LoggerFactory.getLogger(PickPileUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, userId: String, request: PickPileRequest): PickPileResponse {
        try {
            itemLockService.lock(request.pileCode)   // Lock the pile
            val dataHolder = pick(sessionId, userId, request)
            if (dataHolder.errorType != PileError.NONE) {
                return PickPileResponse.Failure(
                    errorType = dataHolder.errorType,
                    errorMessage = dataHolder.errorMessage,
                    goodMovement = dataHolder.goodMovement?.toPickedGoodMovement(),
                    items = dataHolder.pickedItems
                )
            }

            return PickPileResponse.Success(
                goodMovement = dataHolder.goodMovement!!.toPickedGoodMovement(),
                items = dataHolder.pickedItems,
                pileCount = dataHolder.pileCount,
                itemCount = dataHolder.itemCount
            )
        } finally {
            itemLockService.unlock(request.pileCode)   // Unlock the pile
        }
    }

    fun pick(sessionId: String, userId: String, request: PickPileRequest): DataHolder {
        Database.connect(dataSource)

        var errorType = PileError.NONE
        var errorMessage = ""
        var pickedItems: List<MovingItem> = listOf()
        lateinit var goodMovement: GoodMovementDao
        lateinit var pile: PileDao
        var lotNoIds = listOf<Int>()
        transaction {
            addLogger(exposedLogger)
            val pair = pileService.findPileAndCurrentLotNos(request.pileCode)
            if (pair == null) {
                errorType = PileError.PILE_NOT_FOUND
                logger.warn("Pile: ${request.pileCode} not found")
                return@transaction
            }

            pile = pair.first
            lotNoIds = pair.second.map { it.id.value }
            pickedItems = pileService.findItemsInStorageArea(lotNoIds)
            if (pickedItems.isEmpty()) {
                errorType = PileError.LOT_HAS_ZERO_AMOUNT
                errorMessage = "Pile: ${pile.code} does't have any stock in storeLocation table."
                logger.warn(errorMessage)
                return@transaction
            }

            goodMovement = GoodMovementDao.findById(request.goodMovementId)!!
            if (goodMovement.approveUserId == null) {
                errorType = PileError.GOOD_MOVEMENT_IS_NOT_APPROVED
                errorMessage = "Pile: ${pile.code}, Good movement ${goodMovement.code} is not approved."
                logger.warn(errorMessage)
                return@transaction
            }

            if (goodMovement.type == GoodMovementType.PICKING_ORDER.wmsName) {
                val pileDepartmentId = storeLocationRepository.getDepartmentId(pile.storeLocationId)
                val gmDeptId = goodMovement.departmentId.value
                if (pileDepartmentId != gmDeptId && !goingToKilnDry(pileDepartmentId, gmDeptId)) {
                    errorType = PileError.UNABLE_TO_PICK_BECAUSE_NOT_IN_LOCATION
                    errorMessage = "Pile: ${pile.code}, The current location ${pile.storeLocationId} is not the same as department id."
                    logger.warn(errorMessage)
                    return@transaction
                }
            }
        }

        if (errorType != PileError.NONE) {
            logger.warn("Pile ${request.pileCode}: Error: $errorMessage")
            return DataHolder(
                errorType = errorType,
                errorMessage = errorMessage,
                goodMovement = goodMovement,
                pickedItems = pickedItems
            )
        }

        val itemMovements = createItemMovements(request, pickedItems)
        val result = wmsService.pickGmItem(sessionId, itemMovements)
        if (result is ResultOf.Failure) {
            logger.warn("Pile ${request.pileCode}: WMS validation error: ${result.message}")
            return DataHolder(
                errorType = PileError.WMS_VALIDATION_ERROR,
                errorMessage = result.message!!,
                goodMovement = goodMovement,
                pickedItems = pickedItems
            )
        }

        var pileCount = 0
        var itemCount = 0
        transaction {
            addLogger(exposedLogger)
            // อันนี้เป็นการทดสอบซ้ำว่าการเรียก wmsService.pickGmitem มีการทำจริงมั้ย (เพราะเจอว่ามีไม่ได้ทำด้วย)
            // ถ้าเจอว่าไม่ได้ทำ ก็จะให้ Fail ก่อน หน้างานจะได้ลองใหม่หรือไม่ก็ติดต่อมา
            val supposeToPickItems = pileService.findItemsInStorageArea(lotNoIds)
            if (supposeToPickItems.isNotEmpty()) {
                logger.warn("Pile ${request.pileCode}: WMS validation error: Unable to perform pick operation in WMS.")
                errorType = PileError.WMS_VALIDATION_ERROR
                return@transaction
            }
            upsertPileRecords(userId, request, pile, pickedItems)
            pileService.getPileAndItemCount(request.goodMovementId).apply {
                pileCount = this.first
                itemCount = this.second
            }
        }

        if (errorType != PileError.NONE) {
            return DataHolder(
                errorType = PileError.WMS_VALIDATION_ERROR,
                goodMovement = goodMovement,
                pickedItems = pickedItems
            )
        }

        return DataHolder(
            pile = pile,
            pickedItems = pickedItems,
            goodMovement = goodMovement,
            pileCount = pileCount,
            itemCount = itemCount
        )
    }

    private fun createItemMovements(request: PickPileRequest, items: List<MovingItem>): List<WmsService.ItemMovementEntry> {
        return items.map { pickingData ->
            val lotName = "${pickingData.lotCode} | ${pickingData.matCode} | ${pickingData.lotRefCode} | ${pickingData.storeLocationCode}"

            WmsService.ItemMovementEntry(
                goodMovementType = GoodMovementType.PICKING_ORDER,
                goodMovementId = request.goodMovementId,
                skuId = pickingData.skuId,
                storeLocationId = pickingData.storeLocationId,
                manufacturingLineId = request.manufacturingLineId,
                qty = pickingData.qty.setScale(2, RoundingMode.HALF_UP),
                lotNoId = pickingData.lotNoId,
                lotNo = lotName
            )
        }
    }

    private fun upsertPileRecords(userId: String, request: PickPileRequest, pile: PileDao, pickedItems: List<MovingItem>) {
        // Update the pile to the current good movement.
        Pile.update({Pile.id eq pile.id.value}) {
            it[this.goodMovementId] = request.goodMovementId
            it[this.updatedAt] = LocalDateTime.now()
        }

        // Record Pile pick for process transaction
        pileService.addPileTransaction(
            pileId = pile.id.value,
            fromGoodMovementId = pile.goodMovementId.value,
            toGoodMovementId = request.goodMovementId,
            userId = userId.toInt(),
            type = request.transactionType,
            fromLotNos = pickedItems.map { item -> item.lotNoId },
            movingQty = pickedItems.map { item -> item.qty },
            remainingQty = List(pickedItems.size) { 0.toBigDecimal() },
        )
    }

    /**
     * อันนี้เป็นกรณีพิเศษที่ไม่ต้องเช็คเรื่องที่ว่ากองไม้อยู่ที่แผนกที่ต้องการเบิกเข้าผลิตแล้วมั้ย เพราะถ้าต้องไปให้ยิงเข้าโลเคชั่นของเตาอบก่อนมันค่อนข้าง
     * ยุ่งยากสำหรับหน้างาน
     */
    private fun goingToKilnDry(pileDepartmentId: Int, goodMovementDepartmentId: Int): Boolean {
        return pileDepartmentId == ACQ_DEPARTMENT_ID && goodMovementDepartmentId == KILN_DRY_DEPARTMENT_ID
    }

    data class DataHolder(
        val pile: PileDao? = null,
        val pickedItems: List<MovingItem> = listOf(),
        val goodMovement: GoodMovementDao? = null,
        val errorType: PileError = PileError.NONE,
        val errorMessage: String = "",
        val pileCount: Int = 0,
        val itemCount: Int = 0,
    ) {
        fun hasError(): Boolean {
            return errorType != PileError.NONE
        }

    }
}

fun GoodMovementDao.toPickedGoodMovement(): PickedGoodMovement {
    return PickedGoodMovement(
        code = this.code,
        poNo = this.poNo,
        jobNo = this.jobNo,
        invoiceNo = this.invoiceNo,
        orderNo = this.orderNo,
        productionDate = this.productionDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    )
}