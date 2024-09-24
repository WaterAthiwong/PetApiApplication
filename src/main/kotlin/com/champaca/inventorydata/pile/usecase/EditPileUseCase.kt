package com.champaca.inventorydata.pile.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.storelocation.StoreLocationRepository
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.request.EditPileRequest
import com.champaca.inventorydata.pile.response.EditPileResponse
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class EditPileUseCase(
    val dataSource: DataSource,
    val pileService: PileService,
    val wmsService: WmsService,
    val storeLocationRepository: StoreLocationRepository
) {

    val logger = LoggerFactory.getLogger(EditPileUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    /**
     * Editing condition
     * 1. Pile must exist
     * 2. Good Movement must be of type 'good receipt'
     * 3. Will work the current lot set only.
     */

    fun execute(sessionId: String, userId: String, request: EditPileRequest): EditPileResponse {
        Database.connect(dataSource)

        var errorType = PileError.NONE
        var toBeRemoveGmItems: List<GmItemDao> = listOf()
        var initialLotIds = listOf<LotNoDao>()
        lateinit var storeLocation: StoreLocationDao
        lateinit var goodMovement: GoodMovementDao
        lateinit var pile: PileDao
        var newStartIndex = 1
        transaction {
            addLogger(exposedLogger)

            val pair = pileService.findPileAndCurrentLotNos(request.pileCode)
            if (pair == null) {
                errorType = PileError.PILE_NOT_FOUND
                logger.warn("Pile: ${request.pileCode} not found")
                return@transaction
            }

            pile = pair.first
            goodMovement = pile.goodMovement
            initialLotIds = pair.second
            if (pile.goodMovementId.value != pile.originGoodMovementId) {
                errorType = PileError.UNABLE_TO_EDIT_PILE_BECAUSE_BEEN_USED
                logger.warn("Pile: ${request.pileCode} has been used. Unable to edit.")
                return@transaction
            }

            if (!request.toBeRemovedLotNos.isNullOrEmpty()) {
                toBeRemoveGmItems = pileService.findGmItems(pile.goodMovementId.value, request.toBeRemovedLotNos)
            }

            storeLocation = storeLocationRepository.getById(pile.storeLocationId)!!
            newStartIndex = findNewLotIndex(pile)
        }

        if (errorType != PileError.NONE) {
            return EditPileResponse.Failure(errorType, "")
        }

        val lotGroupRefCode = initialLotIds[0].refCode.dropLast(2)

        if (!request.toBeRemovedLotNos.isNullOrEmpty()) {
            // Remove items for this pile from WMS
            val removeResult = wmsService.removeGmItem(sessionId, toBeRemoveGmItems.map { it.id.value })
            if (removeResult is ResultOf.Failure) {
                logger.warn("Pile: ${request.pileCode}: WMS validation error: ${removeResult.message}")
                return EditPileResponse.Failure(PileError.WMS_VALIDATION_ERROR, removeResult.message)
            }
        }

        if (!request.toBeAddedItems.isNullOrEmpty()) {
            // Add items for this pile to WMS
            val itemMovements =
                createItemMovements(request, request.remark, goodMovement, storeLocation, lotGroupRefCode, newStartIndex)
            val addResult = wmsService.receiveGmItem(sessionId, itemMovements)
            if (addResult is ResultOf.Failure) {
                logger.warn("Pile: ${request.pileCode}: WMS validation error: ${(addResult as ResultOf.Failure).message}")
                return EditPileResponse.Failure(PileError.WMS_VALIDATION_ERROR, addResult.message)
            }
        }

        transaction {
            addLogger(exposedLogger)
            val lotNoIds = getLotNoFromRefCode(lotGroupRefCode).map { it.id.value }
            val qtys = getLotNoQty(lotNoIds)
            upsertPileRecords(userId, request, pile, initialLotIds.map { it.id.value }, lotNoIds, qtys)
        }

        return EditPileResponse.Success(
            removedItems = toBeRemoveGmItems.size,
            addedItems = request.toBeAddedItems?.size ?: 0)
    }

    private fun getLotNoFromRefCode(lotGroupRefCode: String): List<LotNoDao> {
        return LotNoDao.find { (LotNo.refCode.like("${lotGroupRefCode}_%")) and (LotNo.status eq "A") }.toList()
    }

    private fun findNewLotIndex(pile: PileDao): Int {
        val query = PileHasLotNo.select(PileHasLotNo.lotNoId.count())
            .where { (PileHasLotNo.pileId eq pile.id) and (PileHasLotNo.lotSet eq pile.lotSet) }
        val result = query.map { it[PileHasLotNo.lotNoId.count()] }.firstOrNull()
        return if (result == null) 1 else result.toInt() + 1
    }

    private fun getLotNoQty(lotNoIds: List<Int>): List<BigDecimal> {
        val query = StoreLocationHasLotNo.select(StoreLocationHasLotNo.lotNoId, StoreLocationHasLotNo.qty)
            .where { StoreLocationHasLotNo.lotNoId.inList(lotNoIds) }
        val queryResults = query.map { Pair(it[StoreLocationHasLotNo.lotNoId].value, it[StoreLocationHasLotNo.qty]) }
            .associateBy({it.first}, {it.second})
        val results = mutableListOf<BigDecimal>()
        lotNoIds.forEach {
            results.add(queryResults[it] ?: 0.toBigDecimal())
        }
        return results
    }

    private fun createItemMovements(request: EditPileRequest,
                                    remark: String?,
                                    goodMovement: GoodMovementDao,
                                    storeLocation: StoreLocationDao,
                                    logGroupRefCode: String,
                                    startIndex: Int): List<WmsService.ItemMovementEntry> {
        return request.toBeAddedItems!!.mapIndexed { index, item ->
            WmsService.ItemMovementEntry(
                goodMovementType = GoodMovementType.GOODS_RECEIPT,
                goodMovementId = goodMovement.id.value,
                skuId = item.skuId,
                sku = item.matCode,
                storeLocationId = storeLocation.id.value,
                storeLocation = storeLocation.code,
                manufacturingLineId = goodMovement.manufacturingLineId,
                qty = item.qty.setScale(2, RoundingMode.HALF_UP),
                refCode = "${logGroupRefCode}${(index + startIndex).toString().padStart(2, '0')}",
                remark = remark
            )
        }
    }

    private fun upsertPileRecords(userId: String,
                                  request: EditPileRequest,
                                  pile: PileDao,
                                  initialLotNoIds: List<Int>,
                                  newLotNoIds: List<Int>,
                                  qtys: List<BigDecimal>){
        val now = LocalDateTime.now()

        Pile.update({ Pile.id eq pile.id }) {
            if (!request.orderNo.isNullOrEmpty()) {
                it[orderNo] = request.orderNo
            }
            if (!request.refNo.isNullOrEmpty()||!request.customer.isNullOrEmpty()) {
                val extra = pile.extraAttributes?.toMutableMap() ?: mutableMapOf()
                if(!request.refNo.isNullOrEmpty()) {
                    extra["refNo"] = request.refNo
                }
                if(!request.customer.isNullOrEmpty()) {
                    extra["customer"] = request.customer
                }
                it[extraAttributes] = extra
            }
            if (!request.remark.isNullOrEmpty()) {
                it[remark] = request.remark
            }
            it[updatedAt] = now
        }
        pileService.addPileHasLotNos(pile.id.value, newLotNoIds.filter { !initialLotNoIds.contains(it) }, pile.lotSet)
        pileService.addPileTransaction(pileId = pile.id.value,
            fromGoodMovementId = pile.goodMovementId.value,
            toGoodMovementId = pile.goodMovementId.value,
            userId = userId.toInt(),
            type = PileTransactionDao.EDIT,
            fromLotNos = initialLotNoIds,
            toLotNos = newLotNoIds,
            remainingQty = qtys,
            remark = request.reason
        )
    }
}