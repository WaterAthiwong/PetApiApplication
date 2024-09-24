package com.champaca.inventorydata.fg.flooring.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.Pile
import com.champaca.inventorydata.databasetable.PileHasLotNo
import com.champaca.inventorydata.databasetable.StoreLocationHasLotNo
import com.champaca.inventorydata.databasetable.dao.PileDao
import com.champaca.inventorydata.databasetable.dao.PileTransactionDao
import com.champaca.inventorydata.fg.flooring.request.JoinPileRequest
import com.champaca.inventorydata.fg.flooring.response.JoinPileResponse
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class JoinPileUseCase(
    val dataSource: DataSource,
    val pileService: PileService
) {
    val logger = LoggerFactory.getLogger(JoinPileUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(userId: String, request: JoinPileRequest): JoinPileResponse {
        Database.connect(dataSource)

        var errorType = PileError.NONE
        var errorMessage = ""
        transaction {
            addLogger(exposedLogger)

            val nonMatchings = request.joinPileCodes.filter { !it.startsWith(request.mainPileCode) }
            if (nonMatchings.size > 0) {
                errorType = PileError.INVALID_REQUEST
                errorMessage = "Some join piles ${nonMatchings} do not match main pile code ${request.mainPileCode}"
                logger.warn(errorMessage)
                return@transaction
            }

            val mainPile = PileDao.find { (Pile.code eq request.mainPileCode) and (Pile.status eq "A") }.firstOrNull()
            if (mainPile == null) {
                errorType = PileError.PILE_NOT_FOUND
                errorMessage = "Main pile ${request.mainPileCode} not found"
                logger.warn(errorMessage)
                return@transaction
            }

            val joinPiles = PileDao.find { (Pile.code inList request.joinPileCodes) and (Pile.status eq "A") }.toList()
            if (joinPiles.size != request.joinPileCodes.size) {
                errorType = PileError.PILE_NOT_FOUND
                errorMessage = "Some join piles ${request.joinPileCodes} not found"
                logger.warn(errorMessage)
                return@transaction
            }

            // ที่ต้องทำการ upsert ข้อมูล transaction ก่อนที่จะทำการย้าย item จริงๆเพราะว่า ไม่อย่างนั้น จะทำให้ข้อมูล transaction ไม่ถูกต้อง
            upsertRecord(userId, mainPile, joinPiles)
            moveItems(mainPile, joinPiles)
        }

        if (errorType != PileError.NONE) {
            return JoinPileResponse.Failure(errorType, errorMessage)
        }

        return JoinPileResponse.Success(true)
    }

    private fun moveItems(mainPile: PileDao, joinPiles: List<PileDao>) {
        PileHasLotNo.update({ PileHasLotNo.pileId inList joinPiles.map { it.id } }) {
            it[pileId] = mainPile.id
            it[lotSet] = mainPile.lotSet
        }
    }

    private fun upsertRecord(userId: String, mainPile: PileDao, joinPiles: List<PileDao>) {
        val joins = StoreLocationHasLotNo.join(PileHasLotNo, JoinType.INNER) { StoreLocationHasLotNo.lotNoId eq PileHasLotNo.lotNoId }
            .join(Pile, JoinType.INNER) { (Pile.id eq PileHasLotNo.pileId) and (PileHasLotNo.lotSet eq Pile.lotSet) }
        val query = joins.select(PileHasLotNo.lotNoId, StoreLocationHasLotNo.qty, Pile.code)
            .where{ (Pile.code inList joinPiles.map { it.code }) and (Pile.status eq "A") }
        val results = query.toList()

        pileService.addPileTransaction(
            pileId = mainPile.id.value,
            userId = userId.toInt(),
            type = PileTransactionDao.RECEIVE_TRANSFER,
            toLotNos = results.map { it[PileHasLotNo.lotNoId].value },
            movingQty = results.map { it[StoreLocationHasLotNo.qty] },
            fromPiles = joinPiles.map { it.code }
        )

        results.groupBy { it[Pile.code] }.forEach { (pileCode, items) ->
            val joinPile = joinPiles.find { it.code == pileCode }!!
            pileService.addPileTransaction(
                pileId = joinPile.id.value,
                userId = userId.toInt(),
                type = PileTransactionDao.TRANSFER,
                fromLotNos = items.map { it[PileHasLotNo.lotNoId].value },
                movingQty = items.map { it[StoreLocationHasLotNo.qty] },
                toPileId = mainPile.id.value
            )
        }
    }
}