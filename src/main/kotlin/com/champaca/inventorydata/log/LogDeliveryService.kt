package com.champaca.inventorydata.log

import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.log.model.LogDeliveryData
import com.champaca.inventorydata.log.model.LogData
import com.champaca.inventorydata.log.model.ReceivedLogIncidentData
import com.champaca.inventorydata.masterdata.storelocation.StoreLocationRepository
import kotlinx.serialization.descriptors.serialDescriptor
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class LogDeliveryService(
    val storeLocationRepository: StoreLocationRepository
) {
    fun getLogDelivery(search: LogDeliverySearchParams): List<LogDeliveryData> {
        val join = LogDelivery.join(Supplier, JoinType.INNER) { LogDelivery.supplierId eq Supplier.id }
        val query = join.slice(Supplier.name, LogDelivery.id, LogDelivery.supplierId, LogDelivery.poNo,
            LogDelivery.deliveryRound, LogDelivery.forestryBook, LogDelivery.forestryBookNo, LogDelivery.fsc,
            LogDelivery.lotNo, LogDelivery.invoiceNo, LogDelivery.fscNo)
            .select { LogDelivery.status eq "A" }
            .orderBy(LogDelivery.createdAt to SortOrder.DESC)

        if (search.supplierId != null) {
            query.andWhere { LogDelivery.supplierId eq search.supplierId }
        }
        if (!search.poNo.isNullOrEmpty()) {
            query.andWhere { LogDelivery.poNo eq search.poNo }
        }
        if (search.deliveryRound != null) {
            query.andWhere { LogDelivery.deliveryRound eq search.deliveryRound }
        }
        if (!search.forestryBook.isNullOrEmpty()) {
            query.andWhere { LogDelivery.forestryBook eq search.forestryBook }
        }
        if (!search.forestryBookNo.isNullOrEmpty()) {
            query.andWhere { LogDelivery.forestryBookNo eq search.forestryBookNo }
        }
        if (search.fsc != null) {
            query.andWhere { LogDelivery.fsc eq search.fsc }
        }
        if (!search.ids.isNullOrEmpty()) {
            query.andWhere { LogDelivery.id.inList(search.ids) }
        }
        if (!search.invoiceNo.isNullOrEmpty()) {
            query.andWhere { LogDelivery.invoiceNo eq search.invoiceNo }
        }
        if (!search.fscNo.isNullOrEmpty()) {
            query.andWhere { LogDelivery.fscNo eq search.fscNo }
        }
        if (!search.lotNo.isNullOrEmpty()) {
            query.andWhere { LogDelivery.lotNo eq search.lotNo }
        }
        if (!search.createdAtFrom.isNullOrEmpty()) {
            query.andWhere { LogDelivery.createdAt.date() greaterEq LocalDate.parse(search.createdAtFrom) }
        }
        if (!search.createdAtTo.isNullOrEmpty()) {
            query.andWhere { LogDelivery.createdAt.date() lessEq LocalDate.parse(search.createdAtTo) }
        }
        return query.map { resultRow ->
            LogDeliveryData(
                supplierId = resultRow[LogDelivery.supplierId],
                poNo = resultRow[LogDelivery.poNo],
                deliveryRound = resultRow[LogDelivery.deliveryRound],
                forestryBook = resultRow[LogDelivery.forestryBook],
                forestryBookNo = resultRow[LogDelivery.forestryBookNo],
                lotNo = resultRow[LogDelivery.lotNo],
                fsc = resultRow[LogDelivery.fsc],
                invoiceNo = resultRow[LogDelivery.invoiceNo],
                fscNo = resultRow[LogDelivery.fscNo]
            ).apply {
                id = resultRow[LogDelivery.id].value
                supplierName = resultRow[Supplier.name]
            }
        }
    }

    fun insertLogDelivery(logDelivery: LogDeliveryData): Int {
        val id = LogDelivery.insertAndGetId {
            it[supplierId] = logDelivery.supplierId
            it[poNo] = logDelivery.poNo
            it[deliveryRound] = logDelivery.deliveryRound
            it[forestryBook] = logDelivery.forestryBook
            it[forestryBookNo] = logDelivery.forestryBookNo
            it[lotNo] = logDelivery.lotNo
            it[fsc] = logDelivery.fsc
            it[invoiceNo] = logDelivery.invoiceNo
            it[fscNo] = logDelivery.fscNo
            it[createdAt] = LocalDateTime.now()
            it[status] = "A"
        }
        return id.value
    }

    fun hasAnyLogEverBeenReceived(deliveryId: Int): List<Int> {
        val query = Log.slice(Log.id)
            .select { (Log.status eq "A") and (Log.logDeliveryId eq deliveryId) and (Log.receivedAt.isNotNull()) }
        return query.map { resultRow -> resultRow[Log.id].value }
    }

    fun insertLogs(logDatas: List<LogData>): Int {
        val ids = Log.batchInsert(logDatas) { logData ->
            this[Log.logDeliveryId] = logData.logDeliveryId
            if (logData.receivingUserId > 0) {
                this[Log.receivingUserId] = logData.receivingUserId
            }
            if (logData.exportingUserId > 0) {
                this[Log.exportingUserId] = logData.exportingUserId
            }
            if (logData.goodsMovementId > 0) {
                this[Log.goodsMovementId] = logData.goodsMovementId
            }
            this[Log.itemNo] = logData.itemOrder
            this[Log.batchNo] = logData.batchNo
            this[Log.species] = logData.species
            this[Log.length] = logData.length
            this[Log.circumference] = logData.circumference
            this[Log.volumnM3] = logData.volumnM3
            this[Log.logNo] = logData.logNo
            this[Log.refCode] = logData.refCode
            this[Log.matCode] = logData.matCode
            if (logData.errorType != null) {
                this[Log.errorCode] = logData.errorType!!.code
            }
            if (logData.receivedAt != null) {
                this[Log.receivedAt] = logData.receivedAt!!
            }
            if (logData.exportedToWmsAt != null) {
                this[Log.exportedToWmsAt] = logData.exportedToWmsAt!!
            }
            this[Log.createdAt] = LocalDateTime.now()
            this[Log.updatedAt] = LocalDateTime.now()
            this[Log.status] = "A"
        }
        return ids.size
    }

    fun updateLog(logData: LogData): Int {
        return Log.update({ Log.id eq logData.id }) {
            it[logDeliveryId] = logData.logDeliveryId
            if (logData.receivingUserId > 0) {
                it[receivingUserId] = logData.receivingUserId
            }
            if (logData.exportingUserId > 0) {
                it[exportingUserId] = logData.exportingUserId
            }
            if (logData.goodsMovementId > 0) {
                it[goodsMovementId] = logData.goodsMovementId
            }
            if (logData.receivedAt != null) {
                it[receivedAt] = logData.receivedAt!!
            }
            if (logData.exportedToWmsAt != null) {
                it[exportedToWmsAt] = logData.exportedToWmsAt!!
            }
            if (logData.errorType != null) {
                it[errorCode] = logData.errorType!!.code
            } else {
                it[errorCode] = null
            }
            it[updatedAt] = LocalDateTime.now()
        }
    }

    fun deactivateLogsByDeliveryId(logDeliveryId: Int): Int {
        return Log.update({ Log.logDeliveryId eq logDeliveryId }) {
            it[status] = "D"
            it[updatedAt] = LocalDateTime.now()
        }
    }

    fun getLogs(search: LogSearchParams): List<LogData> {
        val query = Log.select{ Log.status eq "A" }

        if(!search.ids.isNullOrEmpty()) {
            query.andWhere { Log.id.inList(search.ids) }
        }
        if (!search.logDeliveryIds.isNullOrEmpty()) {
            query.andWhere { Log.logDeliveryId.inList(search.logDeliveryIds) }
        }
        if (search.hasBeenReceived != null) {
            if (search.hasBeenReceived) {
                query.andWhere { Log.receivedAt.isNotNull() }
            } else {
                query.andWhere { Log.receivedAt.isNull() }
            }
        }
        if (!search.receivedFrom.isNullOrEmpty()) {
            query.andWhere { Log.receivedAt.date() greaterEq LocalDate.parse(search.receivedFrom) }
        }
        if (!search.receivedTo.isNullOrEmpty()) {
            query.andWhere { Log.receivedAt.date() lessEq LocalDate.parse(search.receivedTo) }
        }
        if (!search.logNos.isNullOrEmpty()) {
            query.andWhere { Log.logNo.inList(search.logNos) }
        }
        if (!search.refCodes.isNullOrEmpty()) {
            query.andWhere { Log.refCode.inList(search.refCodes) }
        }
        if (search.includeErrors != null) {
            if (search.includeErrors) {
                query.andWhere { Log.errorCode.inList(search.errorCodes) }
            } else {
                query.andWhere { Log.errorCode.isNull() }
            }
        }
        if (search.receivedByUserId != null) {
            query.andWhere { Log.receivingUserId eq search.receivedByUserId }
        }

        return query.map { resultRow ->
            val storeLocationId = resultRow[Log.storeLocationId] ?: -1
            val storeLocation = if (storeLocationId != -1) storeLocationRepository.getById(storeLocationId)?.name ?: "" else ""
            LogData(
                logDeliveryId = resultRow[Log.logDeliveryId],
                receivingUserId = resultRow[Log.receivingUserId] ?: -1,
                exportingUserId = resultRow[Log.exportingUserId] ?: -1,
                goodsMovementId = resultRow[Log.goodsMovementId] ?: -1,
                itemOrder = resultRow[Log.itemNo],
                batchNo = resultRow[Log.batchNo],
                species = resultRow[Log.species],
                length = resultRow[Log.length],
                circumference = resultRow[Log.circumference],
                volumnM3 = resultRow[Log.volumnM3],
                logNo = resultRow[Log.logNo],
                refCode = resultRow[Log.refCode],
                matCode = resultRow[Log.matCode],
                errorType = LogDeliveryError.fromCode(resultRow[Log.errorCode]),
                receivedAt = resultRow[Log.receivedAt],
                exportedToWmsAt = resultRow[Log.exportedToWmsAt],
                createdAt = resultRow[Log.createdAt],
                updatedAt = resultRow[Log.updatedAt],
                storeLocationId = storeLocationId,
                storeLocation = storeLocation
            ).apply {
                id = resultRow[Log.id].value
            }
        }
    }

    fun insertReceiveLogIncident(iBarcode: String, iErrorType: LogDeliveryError, iUserId: Int, iLogId: Int = -1): Int {
        val id = ReceivedLogIncident.insertAndGetId {
            it[barcode] = iBarcode
            it[errorCode] = iErrorType.code
            it[userId] = iUserId
            if (iLogId > -1) {
                it[logId] = iLogId
            }
            it[isSolved] = false
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        }
        return id.value
    }

    fun updateReceiveLogIncident(logIncident: ReceivedLogIncidentData): Int {
        return ReceivedLogIncident.update({ ReceivedLogIncident.id eq logIncident.id }) {
            it[isSolved] = logIncident.isSolved
            it[updatedAt] = LocalDateTime.now()
        }
    }

    fun getReceivedLogIncidents(search: ReceivedLogIncidentSearchParams): List<ReceivedLogIncidentData> {
        val query = ReceivedLogIncident.selectAll()
        if (!search.ids.isNullOrEmpty()) {
            query.andWhere { ReceivedLogIncident.id.inList(search.ids) }
        }
        if (!search.createdAtFrom.isNullOrEmpty()) {
            query.andWhere { ReceivedLogIncident.createdAt.date() greaterEq LocalDate.parse(search.createdAtFrom) }
        }
        if (!search.createdAtTo.isNullOrEmpty()) {
            query.andWhere { ReceivedLogIncident.createdAt.date() lessEq LocalDate.parse(search.createdAtTo) }
        }
        if (search.isSolved != null) {
            query.andWhere { ReceivedLogIncident.isSolved eq search.isSolved }
        }
        if (!search.errorTypes.isNullOrEmpty()) {
            query.andWhere { ReceivedLogIncident.errorCode.inList(search.errorTypes.map { it.code }) }
        }
        if (!search.logIds.isNullOrEmpty()) {
            query.andWhere { ReceivedLogIncident.logId.inList(search.logIds) }
        }
        if (search.createdByUserId != null) {
            query.andWhere { ReceivedLogIncident.userId eq search.createdByUserId }
        }
        return query.map { resultRow ->
            ReceivedLogIncidentData(
                userId = resultRow[ReceivedLogIncident.userId],
                logId = resultRow[ReceivedLogIncident.logId],
                barcode = resultRow[ReceivedLogIncident.barcode],
                errorType = LogDeliveryError.fromCode(resultRow[ReceivedLogIncident.errorCode])!!,
                isSolved = resultRow[ReceivedLogIncident.isSolved],
                createdAt = resultRow[ReceivedLogIncident.createdAt],
                updatedAt = resultRow[ReceivedLogIncident.updatedAt]
            ).apply {
                id = resultRow[ReceivedLogIncident.id].value
            }
        }
    }

    fun findMatchingLotNo(refCode: String): List<Int> {
        val query = LotNo.slice(LotNo.id).select { (LotNo.status eq "A") and (LotNo.refCode eq refCode) }
        return query.map { resultRow -> resultRow[LotNo.id].value }
    }
}

data class LogDeliverySearchParams(
    val supplierId: Int? = null,
    val poNo: String? = null,
    val deliveryRound: Int? = null,
    val forestryBook: String? = null,
    val forestryBookNo: String? = null,
    val fsc: Boolean? = null,
    val ids: List<Int> = listOf(),
    val invoiceNo: String? = null,
    val fscNo: String? = null,
    val lotNo: String? = null,
    val createdAtFrom: String? = null,
    val createdAtTo: String? = null,
)

data class LogSearchParams(
    val ids: List<Int> = listOf(),
    val logDeliveryIds: List<Int> = listOf(),
    val hasBeenReceived: Boolean? = null,
    val receivedFrom: String? = null,
    val receivedTo: String? = null,
    val logNos: List<String> = listOf(),
    val refCodes: List<String> = listOf(),
    val includeErrors: Boolean? = null,
    val errorCodes: List<Int> = listOf(),
    val uploadedFrom: String? = null,
    val uploadedTo: String? = null,
    val receivedByUserId: Int? = null,
)

data class ReceivedLogIncidentSearchParams(
    val ids: List<Int> = listOf(),
    val createdAtFrom: String? = null,
    val createdAtTo: String? = null,
    val isSolved: Boolean? = null,
    val errorTypes: List<LogDeliveryError> = listOf(),
    val logIds: List<Int> = listOf(),
    val createdByUserId: Int? = null
)