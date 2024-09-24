package com.champaca.inventorydata.log.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.Log
import com.champaca.inventorydata.databasetable.LogDelivery
import com.champaca.inventorydata.log.LogDeliveryError
import com.champaca.inventorydata.log.LogDeliverySearchParams
import com.champaca.inventorydata.log.LogDeliveryService
import com.champaca.inventorydata.log.excel.LogDetailParser
import com.champaca.inventorydata.log.excel.LogFileRow
import com.champaca.inventorydata.log.model.LogData
import com.champaca.inventorydata.log.request.UploadLogDeliveryFileParams
import com.champaca.inventorydata.log.response.UploadLogDeliveryResponse
import com.champaca.inventorydata.log.response.ValidateForestryFileResponse
import com.champaca.inventorydata.model.Species
import com.champaca.inventorydata.utils.DateTimeUtil
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class UploadLogDeliveryUseCase(
    val dataSource: DataSource,
    val dateTimeUtil: DateTimeUtil,
    val logDetailParser: LogDetailParser,
    val validateForestryFileUseCase: ValidateForestryFileUseCase,
    val logDeliveryService: LogDeliveryService
) {

    @Value("\${champaca.uploads.location}")
    lateinit var uploadRoot: String

    val logger = LoggerFactory.getLogger(UploadLogDeliveryUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(file: MultipartFile, params: UploadLogDeliveryFileParams): UploadLogDeliveryResponse {
        var errorType: UploadLogDeliveryErrorType? = null
        val fileName = "logDelivery_${params.poNo.replace('/', '_')}_${params.deliveryRound}_${dateTimeUtil.getCurrentDateTimeString("yyyy_MM_dd_HH_mm_ss")}.xls"
        val logFileRows = logDetailParser.parseFile(file, params.supplier, uploadRoot, fileName)
        val validateResponse = validateForestryFileUseCase.validate(logFileRows, params.toValidateForestryFileParams())

        Database.connect(dataSource)
        if(!canCreateLogDelivery(params, validateResponse)) {
            return UploadLogDeliveryResponse.Failure(
                type = UploadLogDeliveryErrorType.INVALID_BARCODES,
                wrongFormatRefCodes = validateResponse.wrongFormatRefCodes,
                existingRefCodes = validateResponse.existingRefCodes,
                duplicatedRefCodes = validateResponse.duplicatedRefCodes
            )
        }

        var logDeliveryId = -1
        var insertedTimbersCount: Int = -1
        transaction {
            addLogger(exposedLogger)
            val searchParams = LogDeliverySearchParams(
                poNo = params.poNo,
                deliveryRound = params.deliveryRound,
                forestryBook = params.forestryBook,
                forestryBookNo = params.forestryBookNo
            )
            val logDeliveries = logDeliveryService.getLogDelivery(searchParams)
            if (!logDeliveries.isNullOrEmpty()) {
                logDeliveryId = logDeliveries.single().id
                val receivedLogs = logDeliveryService.hasAnyLogEverBeenReceived(logDeliveries[0].id)
                if (!receivedLogs.isNullOrEmpty()) {
                    errorType = UploadLogDeliveryErrorType.LOGS_HAS_BEEN_RECEIVED
                    return@transaction
                } else {
                    logDeliveryService.deactivateLogsByDeliveryId(logDeliveryId)
                }
            } else {
                logDeliveryId = logDeliveryService.insertLogDelivery(params.toLogDeliveryData())
            }

            val logData = convertToLogData(logDeliveryId, params.fsc, logFileRows, validateResponse.nonExistingMatCodes!!.map { it.matCode })
            insertedTimbersCount = logDeliveryService.insertLogs(logData)
        }

        return if (errorType != null) {
            UploadLogDeliveryResponse.Failure(type = errorType!!)
        } else {
            UploadLogDeliveryResponse.Success(count = insertedTimbersCount)
        }
    }

    private fun canCreateLogDelivery(params: UploadLogDeliveryFileParams, response: ValidateForestryFileResponse): Boolean {
        if (!response.duplicatedRefCodes.isNullOrEmpty() || !response.wrongFormatRefCodes.isNullOrEmpty()) {
            return false
        }

        if (!response.existingRefCodes.isNullOrEmpty()) {
            var result = false
            transaction {
                val inFileRefCodes = response.existingRefCodes.map { it.refCode }.toSet().toList()
                val inLogDeliveryRefCodes = getLogRefCodes(params, inFileRefCodes)
                // This likely means the user intends to replace logs with a new file because all the logs are in
                // cpc_log table but have not been received.
                result = inFileRefCodes.size == inLogDeliveryRefCodes.size
            }
            return result
        }

        return true
    }

    private fun getLogRefCodes(params: UploadLogDeliveryFileParams, inFileRefCodes: List<String>): List<String> {
        val joins = LogDelivery.join(Log, JoinType.INNER) { LogDelivery.id eq Log.logDeliveryId }
        val query = joins.slice(Log.refCode)
            .select { (LogDelivery.poNo eq params.poNo) and (LogDelivery.deliveryRound eq params.deliveryRound) and
                    (LogDelivery.forestryBook eq params.forestryBook) and (LogDelivery.forestryBookNo eq params.forestryBookNo) and
                    (LogDelivery.status eq "A") and (Log.status eq "A") and (Log.receivedAt.isNull()) and
                    (Log.refCode.inList(inFileRefCodes)) }
        return query.map { resultRow -> resultRow[Log.refCode] }
    }

    private fun convertToLogData(logDeliveryId: Int, fsc: Boolean, logFileRows: List<LogFileRow>, nonExistingMatCodes: List<String>): List<LogData> {
        return logFileRows.map {
            val matCode = it.getMatCode(fsc)
            val errorType = if (nonExistingMatCodes.contains(matCode)) LogDeliveryError.NON_EXISTING_MATCODE else null
            LogData(
                logDeliveryId = logDeliveryId,
                batchNo = it.tabName,
                itemOrder = it.order,
                species = Species.PT.name,
                length = it.length,
                circumference = it.circumference,
                volumnM3 = it.volumnM3,
                logNo = it.logNo,
                refCode = it.refCode,
                matCode = it.getMatCode(fsc),
                errorType = errorType,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }
    }
}

enum class UploadLogDeliveryErrorType {
    INVALID_BARCODES,
    LOGS_HAS_BEEN_RECEIVED
}