package com.champaca.inventorydata.log.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.log.LogDeliverySearchParams
import com.champaca.inventorydata.log.LogDeliveryService
import com.champaca.inventorydata.log.LogSearchParams
import com.champaca.inventorydata.masterdata.user.UserRepository
import com.champaca.inventorydata.log.model.UploadedLog
import com.champaca.inventorydata.log.request.GetUploadedLogsRequest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetUploadedLogsUseCase(
    val dataSource: DataSource,
    val logDeliveryService: LogDeliveryService,
    val userRepository: UserRepository
) {
    val logger = LoggerFactory.getLogger(GetUploadedLogsUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: GetUploadedLogsRequest): List<UploadedLog> {
        var results: List<UploadedLog> = listOf()
        Database.connect(dataSource)
        transaction {
            addLogger(exposedLogger)

            if (hasAnyLogDeliveryParams(request)) {
                results = getResultStartFromLogDelivery(request)
            } else {
                results = getResultStartFromLog(request)
            }
        }
        return results
    }

    private fun hasAnyLogDeliveryParams(request: GetUploadedLogsRequest): Boolean {
        return request.poNo != null || request.deliveryRound != null || request.supplierId != null ||
                request.lotNo != null || request.forestryBook != null || request.forestryBookNo != null ||
                request.invoiceNo != null || request.fscNo != null
    }

    fun getResultStartFromLog(request: GetUploadedLogsRequest): List<UploadedLog> {
        var logs = logDeliveryService.getLogs(request.toLogSearchParams())
        if (logs.isNullOrEmpty()) {
            return listOf()
        }

        val logDeliveryIds = logs.map { it.logDeliveryId }.toSet().toList()
        val searchParams = LogDeliverySearchParams(
            ids = logDeliveryIds
        )
        val logDeliveryMap = logDeliveryService.getLogDelivery(searchParams).associateBy({it.id}, {it})
        val matchedLogDeliveryIds = logDeliveryMap.keys.toList()
        val users = userRepository.getAll()

        // In case the user specify PO no or supplier name, we need to filter only logs that match those PO and supplier name as well
        logs = logs.filter { matchedLogDeliveryIds.contains(it.logDeliveryId) }

        return logs.map { log ->
            val logDelivery = logDeliveryMap[log.logDeliveryId]!!
            val userFirstName = users.find { it.id.value == log.receivingUserId }?.firstname ?: ""
            UploadedLog(
                id = log.id,
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
                errorType = log.errorType,
                receivedAt = log.receivedAt,
                receivedBy = userFirstName,
                exportedToWmsAt = log.exportedToWmsAt,
                invoiceNo = logDelivery.invoiceNo,
                lotNo = logDelivery.lotNo
            )
        }
    }

    fun getResultStartFromLogDelivery(request: GetUploadedLogsRequest): List<UploadedLog> {
        val logDeliveryMap = logDeliveryService.getLogDelivery(request.toLogDeliverySearchParams()).associateBy({it.id}, {it})
        if (logDeliveryMap.isEmpty()) {
            return listOf()
        }

        val searchParams = LogSearchParams(
            logDeliveryIds = logDeliveryMap.keys.toList(),
            receivedFrom = request.receivedFrom,
            receivedTo = request.receivedTo
        )
        val logs = logDeliveryService.getLogs(searchParams)
        val users = userRepository.getAll()
        return logs.map { log ->
            val logDelivery = logDeliveryMap[log.logDeliveryId]!!
            val userFirstName = users.find { it.id.value == log.receivingUserId }?.firstname ?: ""
            UploadedLog(
                id = log.id,
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
                errorType = log.errorType,
                receivedAt = log.receivedAt,
                receivedBy = userFirstName,
                exportedToWmsAt = log.exportedToWmsAt,
                invoiceNo = logDelivery.invoiceNo,
                lotNo = logDelivery.lotNo
            )
        }
    }
}