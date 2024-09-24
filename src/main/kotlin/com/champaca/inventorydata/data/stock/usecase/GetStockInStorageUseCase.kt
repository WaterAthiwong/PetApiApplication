package com.champaca.inventorydata.data.stock.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.model.ItemStock
import com.champaca.inventorydata.data.stock.StockService
import com.champaca.inventorydata.data.stock.request.StockInStorageRequest
import com.champaca.inventorydata.data.stock.response.StockResponse
import com.champaca.inventorydata.databasetable.PileRelocation
import com.champaca.inventorydata.databasetable.StoreLocation
import com.champaca.inventorydata.databasetable.StoreZone
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class GetStockInStorageUseCase(
    val dataSource: DataSource,
    val stockService: StockService
) {

    companion object {
        val DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }

    val logger = LoggerFactory.getLogger(GetStockInStorageUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: StockInStorageRequest): StockResponse {
        Database.connect(dataSource)

        var stocks = listOf<ItemStock>()
        transaction {
            addLogger(exposedLogger)
            stocks = stockService.getStockInStorage(request)
            if (!request.excludedLocationPattern.isNullOrEmpty()) {
                stocks = stocks.filter { !it.location?.startsWith(request.excludedLocationPattern)!! }
            }
            if (request.itemOnly) {
                stocks = squashItems(stocks)
            }
            if (request.relocateFromDepartmentIds.isNotEmpty()) {
                stocks = addRelocationDate(stocks, request.relocateFromDepartmentIds)
            }
        }

        return StockResponse(
            stocks = stocks,
            totalPiles = stocks.map { it.pileCode }.distinct().size,
            totalPieces = stocks.sumOf { it.qty },
            totalVolumnFt3 = stocks.sumOf { it.totalVolumnFt3 }
        )
    }

    private fun squashItems(stocks: List<ItemStock>): List<ItemStock> {
        return stocks.groupBy { it.pileCode to it.matCode }.map { (_, items) ->
            if (items.size == 1) {
                return@map items.first()
            }
            items.reduce { acc, item ->
                acc.copy(qty = acc.qty + item.qty)
            }
        }
    }

    private fun addRelocationDate(stocks: List<ItemStock>, relocteFromDepartmentIds: List<Int>): List<ItemStock> {
        val joins = PileRelocation.join(StoreLocation, JoinType.INNER) { PileRelocation.fromStoreLocationId eq StoreLocation.id }
            .join(StoreZone, JoinType.INNER) { StoreLocation.storeZoneId eq StoreZone.id }
        val query = joins.select(PileRelocation.pileId, PileRelocation.createdAt)
            .where { (StoreZone.departmentId inList relocteFromDepartmentIds) and (PileRelocation.pileId inList stocks.map { it.pileId }) }
            .orderBy(PileRelocation.createdAt, SortOrder.DESC)
        val dateByPileId = query.map { it[PileRelocation.pileId] to it[PileRelocation.createdAt] }
            .groupBy { it.first }
            .mapValues { DATETIME_FORMAT.format(it.value.first().second) }
        stocks.forEach { it.relocationDate = dateByPileId[it.pileId] }
        return stocks
    }
}