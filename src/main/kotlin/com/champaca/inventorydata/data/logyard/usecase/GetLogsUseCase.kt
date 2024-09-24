package com.champaca.inventorydata.data.logyard.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.data.logyard.request.GetLogsRequest
import com.champaca.inventorydata.data.logyard.response.GetLogsResponse
import com.champaca.inventorydata.log.LogService
import com.champaca.inventorydata.log.model.StoredLog
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.RoundingMode
import javax.sql.DataSource

@Service
class GetLogsUseCase(
    val dataSource: DataSource,
    val logService: LogService
) {
    val logger = LoggerFactory.getLogger(GetLogsUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: GetLogsRequest): GetLogsResponse {
        var results: List<StoredLog> = listOf()
        Database.connect(dataSource)
        transaction {
            addLogger(exposedLogger)
            results = logService.getStoredLogs(request.toStoredLogSearchParam())
        }
        return GetLogsResponse(
            stocks = results,
            totalLogs = results.size,
            totalVolumnM3 = results.sumOf { it.volumnM3 }.setScale(2, RoundingMode.HALF_UP))
    }
}