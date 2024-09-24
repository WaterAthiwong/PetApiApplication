package com.champaca.inventorydata.log.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.Log
import com.champaca.inventorydata.databasetable.LotNo
import com.champaca.inventorydata.databasetable.ReceivedLogIncident
import com.champaca.inventorydata.databasetable.dao.LogDao
import com.champaca.inventorydata.databasetable.dao.LogDeliveryDao
import com.champaca.inventorydata.databasetable.dao.LotNoDao
import com.champaca.inventorydata.databasetable.dao.ReceivedLogIncidentDao
import com.champaca.inventorydata.log.*
import com.champaca.inventorydata.log.model.UploadedLog
import com.champaca.inventorydata.log.request.ReceiveLogRequest
import com.champaca.inventorydata.log.response.ReceivedLogResponse
import com.champaca.inventorydata.masterdata.sku.SkuRepository
import com.champaca.inventorydata.masterdata.storelocation.StoreLocationRepository
import com.champaca.inventorydata.masterdata.supplier.SupplierRepository
import com.champaca.inventorydata.masterdata.user.UserRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class ReceiveLogUseCase(
    val dataSource: DataSource,
    val logDeliveryService: LogDeliveryService,
    val logService: LogService,
    val userRepository: UserRepository,
    val skuRepository: SkuRepository,
    val storeLocationRepository: StoreLocationRepository,
    val supplierRepository: SupplierRepository
) {
    companion object {
        const val DEFAULT_LOG_YARD_LOCATION = "BSLYZ9999"
        val RECEIVED_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    val logger = LoggerFactory.getLogger(ReceiveLogUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(username: String, request: ReceiveLogRequest): ReceivedLogResponse {
        Database.connect(dataSource)
        var errorType: LogDeliveryError? = null
        var log: LogDao
        var logDelivery: LogDeliveryDao
        var userId = -1
        var userFirstName = ""
        var uploadedLog: UploadedLog? = null
        transaction {
            addLogger(exposedLogger)
            val user = userRepository.findByUsername(username)
            if (user == null) {
                errorType = LogDeliveryError.NO_USERNAME_IN_WMS
                return@transaction
            }

            userId = user.id.value
            userFirstName = user.firstname

            val logs = LogDao.find { (Log.refCode eq request.barcode) and (Log.status eq "A") }.toList()
            // Check whether this barcode has associated data.
            if (logs.isEmpty()) {
                errorType = LogDeliveryError.NON_EXISTING_BARCODE
                logDeliveryService.insertReceiveLogIncident(request.barcode, errorType!!, userId)
                return@transaction
            }

            val storeLocation = storeLocationRepository.getByCode(request.location ?: DEFAULT_LOG_YARD_LOCATION)
            if (storeLocation == null) {
                errorType = LogDeliveryError.LOCATION_NOT_FOUND
                logger.warn("Location: ${request.location} not found")
                return@transaction
            }

            log = logs.single()
            val previouslyReceivingUserFirstName = userRepository.findById(log.receivingUserId)?.firstname ?: ""
            logDelivery = LogDeliveryDao.findById(log.logDeliveryId)!!
            uploadedLog = if (previouslyReceivingUserFirstName.isNotEmpty()) {
                createReceivedLog(logDelivery, log, previouslyReceivingUserFirstName)
            } else {
                createReceivedLog(logDelivery, log, userFirstName)
            }

            val lotNos = LotNoDao.find { (LotNo.refCode eq request.barcode) and (LotNo.status eq "A") }.toList()
            // Check whether this barcode already in WMS system.
            if (lotNos.isNotEmpty()) {
                errorType = LogDeliveryError.ALREADY_IN_WMS
                logDeliveryService.insertReceiveLogIncident(request.barcode, errorType!!, userId, log.id.value)
                return@transaction
            }

            // Check if this log has already been received.
            if (log.receivedAt != null) {
                errorType = LogDeliveryError.ALREADY_RECEIVED
                logDeliveryService.insertReceiveLogIncident(request.barcode, errorType!!, userId, log.id.value)
                return@transaction
            }

            // If the matcode didn't exist in WMS when uploading the file, we have a chance to recheck them again
            val logErrorType = LogDeliveryError.fromCode(log.errorCode)
            if (logErrorType == LogDeliveryError.NON_EXISTING_MATCODE) {
                val nonExistingMatCodes = skuRepository.findNonExistingMatCodes(listOf(log.matCode))
                if (nonExistingMatCodes.contains(log.matCode)) {
                    errorType = LogDeliveryError.NON_EXISTING_MATCODE
                    logDeliveryService.insertReceiveLogIncident(request.barcode, errorType!!, userId, log.id.value)
                    return@transaction
                }
                log.errorCode = null
            }

            log.apply {
                val now = LocalDateTime.now()
                this.storeLocationId = storeLocation.id.value
                this.receivedAt = now
                this.receivingUserId = userId
                this.updatedAt = now
            }
        }

        return if (errorType != null) {
            ReceivedLogResponse.Failure(
                type = errorType!!,
                errorCode = errorType!!.code,
                needToMark = errorType!!.needToMark,
                log = uploadedLog,
                forestryBook = request.forestryBook,
                forestryBookNo = request.forestryBookNo,
                logDeliveryId = request.logDeliveryId,
                stat = findStat(request, userId)
            )
        } else {
            ReceivedLogResponse.Success(
                log = uploadedLog!!,
                forestryBook = request.forestryBook,
                forestryBookNo = request.forestryBookNo,
                logDeliveryId = request.logDeliveryId,
                stat = findStat(request, userId)
            )
        }
    }

    private fun createReceivedLog(logDelivery: LogDeliveryDao, log: LogDao, userFirstName: String): UploadedLog {
        val supplierName = supplierRepository.findById(logDelivery.supplierId)?.name ?: ""
        return  UploadedLog(
            id = log.id.value,
            logDeliveryId = logDelivery.id.value,
            supplierName = supplierName,
            barcode = log.refCode,
            logNo = log.logNo,
            batchNo = log.batchNo,
            poNo = logDelivery.poNo,
            circumference = log.circumference,
            length = log.length,
            matCode = log.matCode,
            volumnM3 = log.volumnM3,
            errorType = LogDeliveryError.fromCode(log.errorCode),
            receivedAt = log.receivedAt,
            receivedBy = userFirstName,
            exportedToWmsAt = log.exportedToWmsAt
        )
    }

    private fun findStat(request: ReceiveLogRequest, userId: Int): ReceivedLogResponse.Stat {
        var progress = Pair(0, 0)
        var nonExistingBarcodes = 0
        transaction {
            addLogger(ExposedInfoLogger)
            progress = findForestryBookProgress(request.logDeliveryId)
            nonExistingBarcodes = findNonExistingBarcodeByDate(request.receivedDate, userId)
        }
        return ReceivedLogResponse.Stat(
            receivedByForestryBook = progress.first,
            totalByForestryBook = progress.second,
            nonExistingBarcodes = nonExistingBarcodes
        )
    }

    private fun findForestryBookProgress(logDeliveryId: Int?): Pair<Int, Int> {
        if (logDeliveryId != null) {
            val logs = LogDao.find { Log.logDeliveryId eq logDeliveryId }.toList()

            // Get the log incident list and make sure there are no duplicates
            val incidents = ReceivedLogIncidentDao.find { (ReceivedLogIncident.logId inList logs.map { it.id.value }) and
                    (ReceivedLogIncident.errorCode eq LogDeliveryError.NON_EXISTING_MATCODE.code) }
                .toList()
            val incidentLogIds = incidents.map { it.logId!! }.toSet().toList()

            val totalLogs = logs.size
            val receivedLogs =
                logs.filter { !incidentLogIds.contains(it.id.value) && it.receivedAt != null }.size + incidentLogIds.size
            return Pair(receivedLogs, totalLogs)
        }

        // Default case, return 0
        return Pair(0, 0)
    }

    private fun findNonExistingBarcodeByDate(date: String, userId: Int): Int {
        val startTime = LocalDate.parse(date, RECEIVED_DATE_FORMAT).atStartOfDay()
        val endTime = LocalDate.parse(date, RECEIVED_DATE_FORMAT).atTime(23, 59, 59)
        val incidents = ReceivedLogIncidentDao.find {
            (ReceivedLogIncident.errorCode eq LogDeliveryError.NON_EXISTING_BARCODE.code) and
                    (ReceivedLogIncident.createdAt greaterEq startTime) and
                    (ReceivedLogIncident.createdAt lessEq endTime) and
                    (ReceivedLogIncident.userId eq userId)
        }.toList()

        return incidents.map { it.barcode }.toSet().size
    }
}