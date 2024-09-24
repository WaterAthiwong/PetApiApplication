package com.champaca.inventorydata.data.stock.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.model.ItemStock
import com.champaca.inventorydata.data.stock.StockService
import com.champaca.inventorydata.data.stock.request.StockInProcessRequest
import com.champaca.inventorydata.data.stock.request.StockInStorageRequest
import com.champaca.inventorydata.data.stock.response.StockResponse
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetStockInProcessUseCase(
    val dataSource: DataSource,
    val stockService: StockService
) {
    val logger = LoggerFactory.getLogger(GetStockInProcessUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: StockInProcessRequest): StockResponse {
        Database.connect(dataSource)

        var stocks = listOf<ItemStock>()
        transaction {
            addLogger(exposedLogger)
            stocks = stockService.getStockInProcess(request)
        }

        return StockResponse(
            stocks = stocks,
            totalPiles = stocks.map { it.pileCode }.distinct().size,
            totalPieces = stocks.sumOf { it.qty },
            totalVolumnFt3 = stocks.sumOf { it.totalVolumnFt3 }
        )
    }
}