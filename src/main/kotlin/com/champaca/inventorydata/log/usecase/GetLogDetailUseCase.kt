package com.champaca.inventorydata.log.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.log.*
import com.champaca.inventorydata.log.response.GetLogDetailResponse
import com.champaca.inventorydata.log.model.StoredLog
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetLogDetailUseCase(
    val dataSource: DataSource,
    val logService: LogService,
    val logDeliveryService: LogDeliveryService
) {

    val logger = LoggerFactory.getLogger(GetLogDetailUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(barcode: String): GetLogDetailResponse {
        var log: StoredLog? = null
        var logStatus: GetLogDetailStatus? = null
        Database.connect(dataSource)
        transaction {
            addLogger(exposedLogger)
            val wmsLogs = logService.getStoredLogs(StoredLogSearchParam(refCodes = listOf(barcode)))
            if (!wmsLogs.isNullOrEmpty()) {
                log = wmsLogs.single()
                logStatus = GetLogDetailStatus.IN_WMS
                return@transaction
            }

            val deliveredLogs = logDeliveryService.getLogs(LogSearchParams(refCodes = listOf(barcode)))
            if (!deliveredLogs.isNullOrEmpty()) {
                val deliveredLog = deliveredLogs.single()
                val logDelivery = logDeliveryService.getLogDelivery(LogDeliverySearchParams(ids = listOf(deliveredLog.logDeliveryId))).single()
                log = StoredLog(
                    matCode = deliveredLog.matCode,
                    length = deliveredLog.length.toBigDecimal(),
                    lengthUom = "cm",
                    circumference = deliveredLog.circumference.toBigDecimal(),
                    circumferenceUom = "cm",
                    grade = "",
                    species = deliveredLog.species,
                    skuVolumnM3 = deliveredLog.volumnM3,
                    skuVolumnFt3 = (0.0).toBigDecimal(),
                    refCode = deliveredLog.refCode,
                    supplierName = logDelivery.supplierName,
                    orderNo = "",
                    invoiceNo = logDelivery.invoiceNo ?: "",
                    poNo = logDelivery.poNo,
                    location = deliveredLog.storeLocation,
                    storeLocationId = deliveredLog.storeLocationId,
                    skuId = -1,
                    lotNo = logDelivery.lotNo ?: "",
                    lotNoId = -1,
                    goodMovementId = -1,
                    goodMovementCode = "",
                    productionDate = "",
                    lotExtraAttributes = mapOf("logNo" to deliveredLog.logNo, "volumnM3" to deliveredLog.volumnM3.toString()),
                )
                logStatus = if (deliveredLog.receivedAt != null) {
                    GetLogDetailStatus.IN_DELIVERY_SYSTEM_RECEIVED
                } else {
                    GetLogDetailStatus.IN_DELIVERY_SYSTEM_NOT_YET_RECEIVE
                }
            }
        }

        return if (log != null) {
            GetLogDetailResponse.Success(log = log!!, status = logStatus!!)
        } else {
            GetLogDetailResponse.Failure(errorType = GetLogDetailStatus.NOT_FOUND)
        }
    }
}

enum class GetLogDetailStatus {
    IN_WMS,
    IN_DELIVERY_SYSTEM_RECEIVED,
    IN_DELIVERY_SYSTEM_NOT_YET_RECEIVE,
    NOT_FOUND
}