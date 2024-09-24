package com.champaca.inventorydata.pile.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.databasetable.dao.PileDao
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.request.StockCountRequest
import com.champaca.inventorydata.pile.response.StockCountResponse
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.sql.DataSource

@Service
class StockCountUseCase(
    val dataSource: DataSource
) {

    companion object {
        const val STOCK_COUNT_PERIOD = 5
    }

    fun execute(userId: String, request: StockCountRequest): StockCountResponse {
        var errorType = PileError.NONE
        var errorMessage = ""

        transaction {
            addLogger(ExposedInfoLogger)

            val pile = PileDao.findById(request.pileId)
            if (pile == null) {
                errorType = PileError.PILE_NOT_FOUND
                errorMessage = "Pile ${request.pileId} not found"
                return@transaction
            }

            val countedAt = pile.countedAt
            if (countedAt != null && ChronoUnit.DAYS.between(countedAt, LocalDateTime.now()).toInt() <= STOCK_COUNT_PERIOD) {
                errorType = PileError.PILE_HAS_BEEN_COUNTED
                errorMessage = "Pile ${pile.code} has been counted at ${pile.countedAt}"
                return@transaction
            }

            pile.countedAt = LocalDateTime.now()
            pile.countedUserId = userId.toInt()
        }

        if (errorType != PileError.NONE) {
            return StockCountResponse.Failure(errorType, errorMessage)
        }

        return StockCountResponse.Success(true)
    }
}