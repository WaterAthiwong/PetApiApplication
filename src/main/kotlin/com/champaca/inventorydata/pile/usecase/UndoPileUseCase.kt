package com.champaca.inventorydata.pile.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.GmItem
import com.champaca.inventorydata.databasetable.LotNo
import com.champaca.inventorydata.databasetable.Pile
import com.champaca.inventorydata.databasetable.PileHasLotNo
import com.champaca.inventorydata.databasetable.dao.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.request.UndoPileRequest
import com.champaca.inventorydata.pile.response.UndoPileResponse
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class UndoPileUseCase(
    val dataSource: DataSource,
    val pileService: PileService,
    val wmsService: WmsService
) {
    val logger = LoggerFactory.getLogger(UndoPileUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(session: String, userId: String, request: UndoPileRequest): UndoPileResponse {
        Database.connect(dataSource)

        var errorType = PileError.NONE
        var errorMessage = ""
        var gmItems = listOf<GmItemDao>()
        lateinit var pile: PileDao
        var lotNos = listOf<LotNoDao>()
        var goodMovement: GoodMovementDao? = null
        transaction {
            addLogger(exposedLogger)

            val pair = pileService.findPileAndCurrentLotNos(request.pileCode)
            if (pair == null) {
                errorType = PileError.PILE_NOT_FOUND
                logger.warn("Pile: ${request.pileCode} not found")
                return@transaction
            }
            pile = pair.first
            lotNos = pair.second
            goodMovement = GoodMovementDao.findById(pile.goodMovementId.value)!!
            gmItems = pileService.findGmItems(pile.goodMovementId.value, lotNos.map { it.id.value })

            if (pile.goodMovementId.value == pile.originGoodMovementId) {
                errorType = PileError.UNABLE_TO_UNDO_BECAUSE_FIRST_TRANSACTION
                logger.warn("Unable to undo pile ${request.pileCode} because it currently is at the first transaction")
                return@transaction
            }
        }

        if (errorType != PileError.NONE) {
            return UndoPileResponse.Failure(errorType, errorMessage)
        }

        val removeResult = wmsService.removeGmItem(session, gmItems.map { it.id.value })
        if (removeResult is ResultOf.Failure) {
            logger.warn("Pile: ${request.pileCode}: WMS validation error: ${removeResult.message}")
            errorType = PileError.WMS_VALIDATION_ERROR
            errorMessage = removeResult.message ?: ""
            return UndoPileResponse.Failure(errorType, errorMessage)
        }

        transaction {
            addLogger(exposedLogger)
            upsertPileRecord(userId, request.reason, pile!!, lotNos, goodMovement!!)
        }

        return UndoPileResponse.Success(request.pileCode)
    }

    private fun upsertPileRecord(userId: String, reason: String, pile: PileDao, lotNos: List<LotNoDao>, goodMovement: GoodMovementDao) {
        val now = LocalDateTime.now()
        val currentGoodMovementId = pile.goodMovementId.value
        if (goodMovement.type == GoodMovementType.PICKING_ORDER.wmsName) {
            // เป็นการเบิก ฉะนั้นถอย GmItem อย่างเดียว
            val previousGmItems = findPreviousGmItem(lotNos)
            Pile.update({ Pile.id eq pile.id.value }) {
                it[this.goodMovementId] = previousGmItems.first().goodMovementId
                it[this.updatedAt] = now
            }
            pileService.addPileTransaction(
                pileId = pile.id.value,
                fromGoodMovementId = currentGoodMovementId,
                toGoodMovementId = previousGmItems.first().goodMovementId,
                userId = userId.toInt(),
                type = PileTransactionDao.UNDO,
                fromLotNos = lotNos.map { it.id.value },
                toLotNos = lotNos.map { it.id.value },
                remainingQty = previousGmItems.map { it.qty },
                remark = reason
            )
        } else {
            // เป็นการรับ ต้องถอย lotset, lotNo ย้อนกลับไป 1 ลอตเซ็ต
            val previousLotNos = findPreviousLotNos(pile)
            val previousGmItems = findPreviousGmItem(previousLotNos)
            Pile.update({ Pile.id eq pile.id.value }) {
                it[this.goodMovementId] = previousGmItems.first().goodMovementId
                it[this.storeLocationId] = previousGmItems.first().storeLocationId
                it[this.lotSet] = pile.lotSet - 1
                it[this.updatedAt] = now
            }
            pileService.addPileTransaction(
                pileId = pile.id.value,
                fromGoodMovementId = currentGoodMovementId,
                toGoodMovementId = previousGmItems.first().goodMovementId,
                userId = userId.toInt(),
                type = PileTransactionDao.UNDO,
                fromLotNos = lotNos.map { it.id.value },
                toLotNos = previousLotNos.map { it.id.value },
                remainingQty = previousGmItems.map { it.qty },
                remark = reason
            )
        }
    }

    private fun findPreviousGmItem(lotNos: List<LotNoDao>): List<GmItemDao> {
        return GmItemDao.find { (GmItem.lotNoId inList lotNos.map { it.id.value }) and (GmItem.status eq "A") }
            .orderBy(GmItem.id to SortOrder.DESC)
            .toList()
    }

    private fun findPreviousLotNos(pile: PileDao): List<LotNoDao> {
        val previousLotSet = pile.lotSet - 1
        val joins = PileHasLotNo.join(LotNo, JoinType.INNER) { PileHasLotNo.lotNoId eq LotNo.id }
        val query = joins.select(LotNo.columns)
            .where { (PileHasLotNo.pileId eq pile.id.value) and (PileHasLotNo.lotSet eq previousLotSet) and
                    (LotNo.status eq "A") }
            .orderBy(LotNo.id to SortOrder.ASC)
        return LotNoDao.wrapRows(query).toList()
    }
}