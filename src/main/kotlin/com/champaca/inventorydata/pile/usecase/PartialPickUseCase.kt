package com.champaca.inventorydata.pile.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.dao.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.request.PartialPickRequest
import com.champaca.inventorydata.pile.response.PartialPickResponse
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.RoundingMode
import javax.sql.DataSource

@Service
class PartialPickUseCase(
    val dataSource: DataSource,
    val pileService: PileService,
    val wmsService: WmsService
) {
    val logger = LoggerFactory.getLogger(PartialPickUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, userId: String, request: PartialPickRequest): PartialPickResponse {
        Database.connect(dataSource)

        var errorType = PileError.NONE
        var errorMessage = ""
        var pickedItems: List<MovingItem> = listOf()
        var goodMovement: GoodMovementDao? = null
        transaction {
            addLogger(exposedLogger)

            // ส่วนนี้เพิ่มเข้ามาเพราะไปเจอว่า ตอนเรียก API มันมี lotNo ของการเรียกคราวที่แล้วติดมาด้วย ทำให้กองก่อนหน้านี้โดนตัดสต้อคเบิ้ล หรือตัดไปสามรอบก็มี
            // ฉะนั้นเลยต้องมีตัวกรองว่าเป็น lotNo ของกองไม้นี้จริงๆเท่านั้น
            val (_, lotNos) = pileService.findPileAndCurrentLotNos(request.pileCode)!!
            val lotNoIds = lotNos.map { it.id.value }
            val requestPickItems = request.pickedItems.filter { it.lotNoId in lotNoIds }

            pickedItems = pileService.findItemsInStorageArea(requestPickItems.map { it.lotNoId })
            if (pickedItems.isEmpty()) {
                errorType = PileError.LOT_HAS_ZERO_AMOUNT
                logger.warn("Pile: ${request.pileCode} doesn't have any stock.")
                return@transaction
            }

            goodMovement = GoodMovementDao.findById(request.fromGoodMovementId)
        }

        if (errorType != PileError.NONE) {
            return PartialPickResponse.Failure(
                errorType = errorType,
                errorMessage = errorMessage,
                goodMovement = goodMovement?.toPickedGoodMovement(),
            )
        }

        val remainingItems = pickedItems.map { it.clone() }
        pickedItems.forEachIndexed { index, item ->
            val toBePickedQty = request.pickedItems.find { it.lotNoId == item.lotNoId }?.qty ?: 0.toBigDecimal()
            item.qty = toBePickedQty
            remainingItems[index].qty -= pickedItems[index].qty
        }

        val itemMovements = createItemMovements(request, pickedItems)
        val result = wmsService.pickGmItem(sessionId, itemMovements)
        if (result is ResultOf.Failure) {
            logger.warn("Pile ${request.pileCode}: WMS validation error: ${result.message}")
            return PartialPickResponse.Failure(
                errorType = PileError.WMS_VALIDATION_ERROR,
                errorMessage = result.message,
                goodMovement = goodMovement?.toPickedGoodMovement()
            )
        }

        transaction {
            addLogger(exposedLogger)
            upsertPileRecords(userId, request, pickedItems, remainingItems)
        }

        return PartialPickResponse.Success(
            pickedItems = pickedItems,
            remainingItems = remainingItems
        )
    }

    private fun createItemMovements(request: PartialPickRequest, items: List<MovingItem>): List<WmsService.ItemMovementEntry> {
        return items.map { pickingData ->
            val lotName = "${pickingData.lotCode} | ${pickingData.matCode} | ${pickingData.lotRefCode} | ${pickingData.storeLocationCode}"

            WmsService.ItemMovementEntry(
                goodMovementType = GoodMovementType.PICKING_ORDER,
                goodMovementId = request.toGoodMovementId,
                skuId = pickingData.skuId,
                storeLocationId = pickingData.storeLocationId,
                manufacturingLineId = request.manufacturingLineId,
                qty = pickingData.qty.setScale(2, RoundingMode.HALF_UP),
                lotNoId = pickingData.lotNoId,
                lotNo = lotName,
                remark = PileTransactionDao.PARTIAL_PICK
            )
        }
    }

    private fun upsertPileRecords(userId: String, request: PartialPickRequest, pickedItems: List<MovingItem>, remainingItems: List<MovingItem>) {
        // Record the partial pick transaction
        pileService.addPileTransaction(pileId = request.pileId,
            fromGoodMovementId = request.fromGoodMovementId,
            toGoodMovementId = request.toGoodMovementId,
            userId = userId.toInt(),
            type = PileTransactionDao.PARTIAL_PICK,
            fromLotNos = pickedItems.map { item -> item.lotNoId },
            movingQty = pickedItems.map { item -> item.qty },
            remainingQty = remainingItems.map { item -> item.qty },
        )
    }
}