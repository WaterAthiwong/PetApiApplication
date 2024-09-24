package com.champaca.inventorydata.log.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.log.LogDeliverySearchParams
import com.champaca.inventorydata.log.LogDeliveryService
import com.champaca.inventorydata.log.response.GetForestryBooksResponse
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetForestryBooksUseCase(
    val dataSource: DataSource,
    val logDeliveryService: LogDeliveryService,
) {
    val logger = LoggerFactory.getLogger(GetForestryBooksUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(): GetForestryBooksResponse {
        var results: MutableMap<String, List<GetForestryBooksResponse.Entry>> = mutableMapOf()
        Database.connect(dataSource)
        transaction {
            addLogger(exposedLogger)
            val logDeliveries = logDeliveryService.getLogDelivery(LogDeliverySearchParams()).groupBy({it.forestryBook}, {it})
            results = mutableMapOf()
            logDeliveries.forEach { key, value ->
                val entries = value.sortedByDescending { it.forestryBookNo }.map {
                    GetForestryBooksResponse.Entry(logDeliveryId = it.id, forestryBookNo = it.forestryBookNo)
                }
                results.put(key, entries)
            }
        }

        return GetForestryBooksResponse(values = results)
    }
}