package com.champaca.inventorydata.conditionroom.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.conditionroom.ConditionRoomService
import com.champaca.inventorydata.conditionroom.model.SuggestedShelf
import com.champaca.inventorydata.conditionroom.request.TransferToProductionRequest
import com.champaca.inventorydata.conditionroom.response.TransferToProductionResponse
import com.champaca.inventorydata.databasetable.ConditionRoomTransfer
import com.champaca.inventorydata.databasetable.Pile
import com.champaca.inventorydata.databasetable.dao.PileDao
import com.champaca.inventorydata.databasetable.dao.PileTransactionDao
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
import java.math.RoundingMode
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class TransferToProductionUseCase(
    val dataSource: DataSource,
    val dateTimeUtil: DateTimeUtil,
    val configRepository: ConfigRepository,
    val wmsService: WmsService,
    val pileService: PileService,
    val itemLockService: ItemLockService,
    val conditionRoomService: ConditionRoomService,
    val storeLocationRepository: StoreLocationRepository
) {

    val logger = LoggerFactory.getLogger(TransferToProductionUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, userId: String, request: TransferToProductionRequest): TransferToProductionResponse {
        val lockName = "goodMovement${request.goodMovementId}"
        try {
            itemLockService.lock(lockName)
            Database.connect(dataSource)

            var errorType = PileError.NONE
            var errorMessage = ""
            var outgoingGoodReceiptId = -1
            lateinit var newPileCode: String
            var wmsTransferringItems: List<WmsService.ItemMovementEntry> = listOf()
            var remainingItems: List<MovingItem> = listOf()
            var deductedItems: List<MovingItem> = listOf()
            var transferringItems: List<MovingItem> = listOf()
            var storeLocationId = -1
            transaction {
                addLogger(exposedLogger)

                remainingItems = conditionRoomService.getRemainingItems(request.goodMovementId)
                val verifyResult = conditionRoomService.verifyTransferInAmount(request.items, remainingItems)
                if (verifyResult is ResultOf.Failure) {
                    errorType = PileError.EXCEEDING_TRANSFERRED_ITEM
                    errorMessage = "Exceeding transferred item: ${verifyResult.message}"
                    logger.warn("Failed to transfer items to production: Exceeds the remaining quantity: ${verifyResult.message}")
                    return@transaction
                }

                val outgoingGoodReceiptResult = conditionRoomService.getIntermediateGoodMovementId("${AssembleUseCase.OUTGIONG_GOOD_RECEIPT_PREFIX}.${request.departmentPrefix}")
                if (outgoingGoodReceiptResult is ResultOf.Failure) {
                    errorType = PileError.INTERMEDIATE_GOOD_MOVEMENT_ID_NOT_FOUND
                    errorMessage = "Config not found for key ${outgoingGoodReceiptResult.message}"
                    logger.warn("Failed to transfer items to production: Config not found for key ${outgoingGoodReceiptResult.message}")
                    return@transaction
                }
                outgoingGoodReceiptId = (outgoingGoodReceiptResult as ResultOf.Success).value

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

                conditionRoomService.deductAndTransfer(request.items, remainingItems).let {
                    remainingItems = it.first
                    transferringItems = it.second
                    wmsTransferringItems = it.second.reduceBySkuId().mapIndexed { index, item ->
                        WmsService.ItemMovementEntry(
                            goodMovementType = GoodMovementType.GOODS_RECEIPT,
                            goodMovementId = outgoingGoodReceiptId,
                            skuId = item.skuId,
                            storeLocationId = storeLocationId,
                            manufacturingLineId = null,
                            qty = item.qty,
                            // At this point the refcode shall be like CO24030123_RM24041562_RM01 where the first part is the
                            // condition room's pile code, the rest is the lot that is transferred to condition room
                            refCode = "${newPileCode}_${request.departmentPrefix}${(index + 1).toString().padStart(2, '0')}",
                        )
                    }
                    deductedItems = it.third
                }
            }

            if (errorType != PileError.NONE) {
                return TransferToProductionResponse.Failure(errorType, errorMessage)
            }

            val result = wmsService.receiveGmItem(sessionId, wmsTransferringItems)
            if (result is ResultOf.Failure) {
                logger.warn("Failed to transfer items to production, error message: $errorMessage")
                return TransferToProductionResponse.Failure(
                    errorType = PileError.WMS_VALIDATION_ERROR,
                    errorMessage = result.message
                )
            }

            var suggestedShelves = listOf<SuggestedShelf>()
            transaction {
                addLogger(exposedLogger)
                wmsTransferringItems = conditionRoomService.fillReceivingLotNoIds(wmsTransferringItems)
                upsertPileData(
                    request = request,
                    newPileCode = newPileCode,
                    outgoingGoodReceiptId = outgoingGoodReceiptId,
                    wmsTransferringItems = wmsTransferringItems,
                    transferringItems = transferringItems,
                    deductedItems = deductedItems,
                    storeLocationId = storeLocationId,
                    userId = userId
                )
                suggestedShelves = conditionRoomService.getSuggestedShelves(remainingItems)
            }

            remainingItems = remainingItems.reduceBySkuId().sortedBy { it.matCode }
            return TransferToProductionResponse.Success(
                remainingItems = remainingItems,
                suggestedShelves = suggestedShelves,
                newPileCode = newPileCode,
                thicknesses = remainingItems.map { it.thickness }.distinct().sorted(),
                widths = remainingItems.map { it.width }.distinct().sorted(),
                lengths = remainingItems.map { it.length }.distinct().sorted(),
                grades = remainingItems.map { it.grade ?: "" }.distinct().sorted()
            )
        } finally {
            itemLockService.unlock(lockName)
        }
    }

    private fun upsertPileData(request: TransferToProductionRequest,
                               newPileCode: String,
                               outgoingGoodReceiptId: Int,
                               wmsTransferringItems: List<WmsService.ItemMovementEntry>,
                               transferringItems: List<MovingItem>,
                               deductedItems: List<MovingItem>,
                               storeLocationId: Int,
                               userId: String) {
        val now = LocalDateTime.now()

        val newPileId = Pile.insertAndGetId {
            it[this.manufacturingLineId] = null
            it[this.goodMovementId] = outgoingGoodReceiptId
            it[this.originGoodMovementId] = outgoingGoodReceiptId
            it[this.storeLocationId] = storeLocationId
            it[this.code] = newPileCode
            it[this.processTypePrefix] = request.departmentPrefix
            it[this.orderNo] = request.orderNo
            it[this.lotSet] = 1
            it[this.type] = PileDao.ASSEMBLED_WOOD_PILE
            it[this.remark] = request.remark
            it[this.createdAt] = now
            it[this.updatedAt] = now
            it[this.status] = "A"
        }
        pileService.addPileHasLotNos(newPileId.value, wmsTransferringItems.map { it.lotNoId!! }, 1)
        val wmsTransferringItemsBySkuId = wmsTransferringItems.associateBy { it.skuId }
        ConditionRoomTransfer.batchInsert(transferringItems) {
            val wmsTransferringItem = wmsTransferringItemsBySkuId[it.skuId]!!
            this[ConditionRoomTransfer.transferGoodMovementId] = request.goodMovementId
            this[ConditionRoomTransfer.transferredLotNoId] = it.lotNoId
            this[ConditionRoomTransfer.newLotNoId] = wmsTransferringItem.lotNoId!!
            this[ConditionRoomTransfer.qty] = it.qty
            this[ConditionRoomTransfer.createdAt] = now
        }

        pileService.addPileTransaction(
            pileId = newPileId.value,
            fromGoodMovementId = request.goodMovementId,
            toGoodMovementId = outgoingGoodReceiptId,
            userId = userId.toInt(),
            type = PileTransactionDao.ASSEMBLE,
            fromLotNos = deductedItems.map { it.lotNoId },
            toLotNos = wmsTransferringItems.map { it.lotNoId!! },
            movingQty = wmsTransferringItems.map { it.qty.setScale(2, RoundingMode.HALF_UP) },
            remainingQty = deductedItems.map { it.qty.setScale(2, RoundingMode.HALF_UP) },
            fromPiles = deductedItems.map { it.pilecode }.distinct()
        )
    }
}