package com.champaca.inventorydata.log.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.log.LogDeliverySearchParams
import com.champaca.inventorydata.log.LogDeliveryService
import com.champaca.inventorydata.log.LogSearchParams
import com.champaca.inventorydata.masterdata.user.UserRepository
import com.champaca.inventorydata.log.model.UploadedLog
import com.champaca.inventorydata.log.request.GetReceivedLogIncidentsRequest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetReceivedLogIncidentsUseCase(
    val dataSource: DataSource,
    val logDeliveryService: LogDeliveryService,
    val userRepository: UserRepository
) {
    val logger = LoggerFactory.getLogger(GetReceivedLogIncidentsUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: GetReceivedLogIncidentsRequest): List<UploadedLog> {
        var results: List<UploadedLog> = listOf()
        Database.connect(dataSource)
        transaction {
            addLogger(exposedLogger)
            val logIncidents = logDeliveryService.getReceivedLogIncidents(request.toReceivedLogIncidentSearchParams())
            val logIds = logIncidents.filter { it.logId != null }.map { it.logId }.toSet().toList() as List<Int>
            val logMap = logDeliveryService.getLogs(LogSearchParams(ids = logIds)).associateBy({it.id}, {it})
            val logDeliveryIds = logMap.values.toList().map { it.logDeliveryId }.toSet().toList()
            val logDeliveryMap = logDeliveryService.getLogDelivery(LogDeliverySearchParams(ids = logDeliveryIds)).associateBy({it.id}, {it})
            val users = userRepository.getAll()

            results = logIncidents.map { logIncident ->
                if (logIncident.logId != null) {
                    val log = logMap[logIncident.logId!!]!!
                    val logDelivery = logDeliveryMap[log.logDeliveryId]!!
                    val userFirstName = users.find { it.id.value == log.receivingUserId }?.firstname ?: ""
                    UploadedLog(
                        id = logIncident.id,
                        logDeliveryId = log.logDeliveryId,
                        supplierName = logDelivery.supplierName,
                        barcode = log.refCode,
                        logNo = log.logNo,
                        batchNo = log.batchNo,
                        poNo = logDelivery.poNo,
                        circumference = log.circumference,
                        length = log.length,
                        matCode = log.matCode,
                        volumnM3 = log.volumnM3,
                        errorType = logIncident.errorType,
                        receivedAt = logIncident.createdAt,
                        receivedBy = userFirstName,
                        exportedToWmsAt = log.exportedToWmsAt
                    )
                } else {
                    UploadedLog(
                        id = logIncident.id,
                        supplierName = "",
                        barcode = logIncident.barcode,
                        logNo = "",
                        batchNo = "",
                        poNo = "",
                        circumference = -1,
                        length = -1,
                        matCode = "",
                        volumnM3 = (0.0).toBigDecimal(),
                        errorType = logIncident.errorType,
                        receivedAt = logIncident.createdAt
                    )
                }
            }
        }
        return results
    }
}