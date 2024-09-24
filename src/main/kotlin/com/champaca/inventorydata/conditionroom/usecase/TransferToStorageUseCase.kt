package com.champaca.inventorydata.conditionroom.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.conditionroom.ConditionRoomService
import com.champaca.inventorydata.conditionroom.model.SuggestedShelf
import com.champaca.inventorydata.conditionroom.request.TransferToStorageRequest
import com.champaca.inventorydata.conditionroom.response.TransferToStorageResponse
import com.champaca.inventorydata.databasetable.ConditionRoomTransfer
import com.champaca.inventorydata.databasetable.Pile
import com.champaca.inventorydata.databasetable.dao.PileDao
import com.champaca.inventorydata.databasetable.dao.PileTransactionDao
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.config.ConfigRepository
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
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class TransferToStorageUseCase(
    val dataSource: DataSource,
    val dateTimeUtil: DateTimeUtil,
    val configRepository: ConfigRepository,
    val wmsService: WmsService,
    val pileService: PileService,
    val itemLockService: ItemLockService,
    val conditionRoomService: ConditionRoomService
) {
    companion object {
        const val INCOMING_GOOD_RECEIPT_PREFIX = "IncomingGoodReceipt"
    }

    val logger = LoggerFactory.getLogger(TransferToStorageUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)
    val refCodeFormat = DateTimeFormatter.ofPattern("yyMMddHHmmss")

    fun execute(sessionId: String, userId: String, request: TransferToStorageRequest): TransferToStorageResponse {
        val lockName = "goodMovement${request.goodMovementId}"
        try {
            itemLockService.lock(lockName)
            Database.connect(dataSource)

            var errorType = PileError.NONE
            var errorMessage = ""
            var incomingGoodReceiptId = -1
            var toPile: PileDao? = null
            var transferringItems: List<WmsService.ItemMovementEntry> = listOf()
            var remainingItems: List<MovingItem> = listOf()
            var deductedItems: List<MovingItem> = listOf()
            transaction {
                addLogger(exposedLogger)

                toPile = PileDao.find { (Pile.code eq request.toPileCode) and (Pile.status eq "A") }.firstOrNull()
                if (toPile == null) {
                    errorType = PileError.PILE_NOT_FOUND
                    logger.warn("Failed to transfer items to pile ${request.toPileCode}: Pile not found")
                    return@transaction
                }

                remainingItems = conditionRoomService.getRemainingItems(request.goodMovementId)
                val verifyResult = conditionRoomService.verifyTransferInAmount(request.items, remainingItems)
                if (verifyResult is ResultOf.Failure) {
                    errorType = PileError.EXCEEDING_TRANSFERRED_ITEM
                    errorMessage = "Exceeding transferred item: ${verifyResult.message}"
                    logger.warn("Failed to transfer items to pile ${request.toPileCode}: Exceeds the remaining quantity: ${verifyResult.message}")
                    return@transaction
                }

                val incomingGoodReceiptResult = conditionRoomService.getIntermediateGoodMovementId("${INCOMING_GOOD_RECEIPT_PREFIX}.${request.departmentPrefix}")
                if (incomingGoodReceiptResult is ResultOf.Failure) {
                    errorType = PileError.INTERMEDIATE_GOOD_MOVEMENT_ID_NOT_FOUND
                    errorMessage = "Config not found for key ${incomingGoodReceiptResult.message}"
                    logger.warn("Failed to transfer items to pile ${request.toPileCode}: Config not found for key ${incomingGoodReceiptResult.message}")
                    return@transaction
                }
                incomingGoodReceiptId = (incomingGoodReceiptResult as ResultOf.Success).value

                conditionRoomService.deductAndTransfer(request.items, remainingItems).let {
                    remainingItems = it.first
                    transferringItems = it.second.map {
                        WmsService.ItemMovementEntry(
                            goodMovementType = GoodMovementType.GOODS_RECEIPT,
                            goodMovementId = incomingGoodReceiptId,
                            skuId = it.skuId,
                            storeLocationId = toPile!!.storeLocationId,
                            manufacturingLineId = null,
                            qty = it.qty,
                            // At this point the refcode shall be like CO24030123_RM24041562_RM01 where the first part is the
                            // condition room's pile code, the rest is the lot that is transferred to condition room
                            refCode = "${toPile!!.code}_${it.lotRefCode}",
                            remark = "${it.gmItemId}"
                        )
                    }
                    deductedItems = it.third
                }
            }

            if (errorType != PileError.NONE) {
                return TransferToStorageResponse.Failure(errorType, errorMessage)
            }

            val result = wmsService.receiveGmItem(sessionId, transferringItems)
            if (result is ResultOf.Failure) {
                logger.warn("Failed to transfer items to pile ${request.toPileCode}, error message: $errorMessage")
                return TransferToStorageResponse.Failure(
                    errorType = PileError.WMS_VALIDATION_ERROR,
                    errorMessage = result.message
                )
            }

            var suggestedShelves = listOf<SuggestedShelf>()
            transaction {
                addLogger(exposedLogger)
                transferringItems = conditionRoomService.fillReceivingLotNoIds(transferringItems)
                upsertPileData(
                    toPile = toPile!!,
                    goodMovementId = request.goodMovementId,
                    incomingGoodReceiptId = incomingGoodReceiptId,
                    remainingItems = remainingItems,
                    transferringItems = transferringItems,
                    deductedItems = deductedItems,
                    userId = userId
                )
                suggestedShelves = conditionRoomService.getSuggestedShelves(remainingItems)
            }

            remainingItems = remainingItems.reduceBySkuId().sortedBy { it.matCode }
            return TransferToStorageResponse.Success(
                remainingItems = remainingItems,
                suggestedShelves = suggestedShelves,
                thicknesses = remainingItems.map { it.thickness }.distinct().sorted(),
                widths = remainingItems.map { it.width }.distinct().sorted(),
                lengths = remainingItems.map { it.length }.distinct().sorted(),
                grades = remainingItems.map { it.grade ?: "" }.distinct().sorted()
            )
        } finally {
            itemLockService.unlock(lockName)
        }
    }

    private fun upsertPileData(toPile: PileDao,
                               goodMovementId: Int,
                               incomingGoodReceiptId: Int,
                               remainingItems: List<MovingItem>,
                               deductedItems: List<MovingItem>,
                               transferringItems: List<WmsService.ItemMovementEntry>,
                               userId: String) {
        val now = LocalDateTime.now()
        Pile.update({ Pile.id eq toPile.id.value }) {
            it[Pile.updatedAt] = now
        }
        pileService.addPileHasLotNos(toPile.id.value, transferringItems.map { it.lotNoId!! }, 1)
        val remainingItemsByRefCode = remainingItems.associateBy { it.lotRefCode }
        ConditionRoomTransfer.batchInsert(transferringItems) {
            val remaining = remainingItemsByRefCode[it.refCode!!.substringAfter("_")]!!
            this[ConditionRoomTransfer.transferGoodMovementId] = goodMovementId
            this[ConditionRoomTransfer.transferredLotNoId] = remaining.lotNoId
            this[ConditionRoomTransfer.newLotNoId] = it.lotNoId!!
            this[ConditionRoomTransfer.qty] = it.qty
            this[ConditionRoomTransfer.createdAt] = now
        }
        pileService.addPileTransaction(
            pileId = toPile.id.value,
            fromGoodMovementId = goodMovementId,
            toGoodMovementId = incomingGoodReceiptId,
            userId = userId.toInt(),
            type = PileTransactionDao.RECEIVE_TRANSFER,
            fromLotNos = deductedItems.map { it.lotNoId },
            toLotNos = transferringItems.map { it.lotNoId!! },
            movingQty = transferringItems.map { it.qty.setScale(2, RoundingMode.HALF_UP) },
            remainingQty = deductedItems.map { it.qty.setScale(2, RoundingMode.HALF_UP) },
            fromPiles = deductedItems.map { it.pilecode }.distinct()
        )
    }
}