package com.champaca.inventorydata.pile.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.dao.GmItemDao
import com.champaca.inventorydata.databasetable.dao.LotNoDao
import com.champaca.inventorydata.databasetable.dao.PileDao
import com.champaca.inventorydata.databasetable.dao.PileTransactionDao
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.request.RemovePileRequest
import com.champaca.inventorydata.pile.response.RemovePileResponse
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class RemovePileUseCase(
    val dataSource: DataSource,
    val pileService: PileService,
    val wmsService: WmsService
) {
    val logger = LoggerFactory.getLogger(RemovePileUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, userId: String, request: RemovePileRequest): RemovePileResponse {
        Database.connect(dataSource)

        var toBeRemoveGmItems = listOf<GmItemDao>()
        var pile: PileDao? = null
        var lotNos = listOf<LotNoDao>()
        var errorType = PileError.NONE
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

            if (pile!!.goodMovementId.value != pile!!.originGoodMovementId) {
                errorType = PileError.UNABLE_TO_REMOVE_PILE_BECAUSE_BEEN_USED
                logger.warn("Pile: ${request.pileCode} has been used. Unable to remove.")
                return@transaction
            }

            toBeRemoveGmItems = pileService.findGmItems(pile!!.goodMovementId.value, lotNos.map { it.id.value })
        }

        if (errorType != PileError.NONE) {
            return RemovePileResponse.Failure(errorType)
        }

        val result = wmsService.removeGmItem(sessionId, toBeRemoveGmItems.map { it.id.value })
        if (result is ResultOf.Failure) {
            logger.warn("Pile ${request.pileCode}: WMS validation error: ${result.message}")
            return RemovePileResponse.Failure(PileError.WMS_VALIDATION_ERROR, result.message)
        }

        transaction {
            addLogger(exposedLogger)
            upsertPileRecord(pile!!, userId, lotNos, request.reason)
        }

        return RemovePileResponse.Success(request.pileCode)
    }

    fun upsertPileRecord(
        pile: PileDao,
        userId: String,
        lotNos: List<LotNoDao>,
        reason: String
    ) {
        val now = LocalDateTime.now()
        pile.apply {
            status = "D"
            updatedAt = now
        }
        pileService.addPileTransaction(
            pileId = pile.id.value,
            fromGoodMovementId = pile.goodMovementId.value,
            userId = userId.toInt(),
            type = PileTransactionDao.REMOVE,
            fromLotNos = lotNos.map { it.id.value },
            remark = reason
        )
    }
}