package com.champaca.inventorydata.log.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ChampacaConstant.FORESTRY_BOOK
import com.champaca.inventorydata.common.ChampacaConstant.FORESTRY_BOOK_NO
import com.champaca.inventorydata.common.ChampacaConstant.LOG_NO
import com.champaca.inventorydata.common.ChampacaConstant.RECEIVED_DATE
import com.champaca.inventorydata.common.ChampacaConstant.SUPPLIER
import com.champaca.inventorydata.common.ChampacaConstant.VOLUMN_M3
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.GoodMovementDao
import com.champaca.inventorydata.databasetable.dao.LogDao
import com.champaca.inventorydata.databasetable.dao.LogDeliveryDao
import com.champaca.inventorydata.databasetable.dao.SkuDao
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.log.LogDeliveryError
import com.champaca.inventorydata.log.request.CreateLogsInWmsRequest
import com.champaca.inventorydata.log.response.CreateLogsInWmsResponse
import com.champaca.inventorydata.masterdata.storelocation.StoreLocationRepository
import com.champaca.inventorydata.masterdata.supplier.SupplierRepository
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class CreateLogsInWmsUseCase(
    val dataSource: DataSource,
    val wmsService: WmsService,
    val supplierRepository: SupplierRepository
) {
    companion object {
        const val ADDITIONAL_FILED_ID = 2
        val receivedDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }

    val logger = LoggerFactory.getLogger(CreateLogsInWmsUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)
    fun execute(sessionId: String, userId: String, request: CreateLogsInWmsRequest): CreateLogsInWmsResponse {
        Database.connect(dataSource)

        var errorType = LogDeliveryError.NONE
        var nonExistingLogsCount = 0
        transaction {
            addLogger(exposedLogger)

            val logs = LogDao.find { Log.id inList request.logIds }.toList()
            val nonExisitngRefCodes = findLogsNotInWms(logs.map { it.refCode })
            val nonExistingLogs = logs.filter { it.refCode in nonExisitngRefCodes }
            val logDeliveryMap = LogDeliveryDao.find { LogDelivery.id inList nonExistingLogs.map { it.logDeliveryId }.distinct()}
                .associateBy { it.id.value }

            if (nonExistingLogs.isNotEmpty()) {
                val matCodeToIdMap = findSkuIdFromMatCodes(nonExistingLogs)
                val goodMovement = GoodMovementDao.find { (GoodMovement.status eq "A") and (GoodMovement.jobNo eq request.jobNo) }.firstOrNull()
                if (goodMovement == null) {
                    errorType = LogDeliveryError.GOOD_MOVEMENT_NOT_FOUND
                    logger.warn("Good movement not found for Job no: ${request.jobNo}")
                    return@transaction
                }
                val itemMovements = createItemMovements(request, nonExistingLogs, logDeliveryMap, matCodeToIdMap, goodMovement)
                val response = wmsService.receiveGmItem(sessionId, itemMovements)
                if (response is ResultOf.Failure) {
                    errorType = LogDeliveryError.WMS_VALIDATION_ERROR
                    logger.warn("WMS validation error: ${response.message}")
                    return@transaction
                }
                val now = LocalDateTime.now()
                nonExistingLogs.forEach { log ->
                    log.apply {
                        goodsMovementId = goodMovement.id.value
                        exportedToWmsAt = now
                        exportingUserId = userId.toInt()
                        updatedAt = now
                    }
                }
                nonExistingLogsCount = nonExistingLogs.size
            }
        }

        return if (errorType != LogDeliveryError.NONE) {
            CreateLogsInWmsResponse.Failure(errorType)
        } else {
            CreateLogsInWmsResponse.Success(nonExistingLogsCount)
        }
    }

    private fun findLogsNotInWms(refCodes: List<String>): List<String> {
        val existingRefCodes = LotNo
            .select(LotNo.refCode)
            .where { (LotNo.refCode inList refCodes) and (LotNo.status eq "A") }
            .map { it[LotNo.refCode] }
        return refCodes.filter { it !in existingRefCodes }
    }

    private fun findSkuIdFromMatCodes(logs: List<LogDao>): Map<String, Int> {
        return SkuDao
            .find { (Sku.matCode inList logs.map { it.matCode }) and (Sku.status eq "A") }
            .map { it.matCode to it.id.value }
            .toMap()
    }

    private fun createItemMovements(request: CreateLogsInWmsRequest,
                                    logs: List<LogDao>,
                                    logDeliveryMap: Map<Int, LogDeliveryDao>,
                                    matCodeToIdMap: Map<String, Int>,
                                    goodMovement: GoodMovementDao): List<WmsService.ItemMovementEntry> {
        return logs.map { log ->
            val logDelivery = logDeliveryMap[log.logDeliveryId]!!
            WmsService.ItemMovementEntry(
                goodMovementType = GoodMovementType.GOODS_RECEIPT,
                goodMovementId = goodMovement.id.value,
                skuId = matCodeToIdMap[log.matCode]!!,
                sku = log.matCode,
                storeLocationId = log.storeLocationId!!,
                storeLocation = request.location,
                manufacturingLineId = goodMovement.manufacturingLineId,
                qty = 1.toBigDecimal(),
                refCode = log.refCode,
                additionalFieldId = ADDITIONAL_FILED_ID,
                additionalFields = mapOf(
                    LOG_NO to log.logNo,
                    VOLUMN_M3 to log.volumnM3.toString(),
                    FORESTRY_BOOK to logDelivery.forestryBook,
                    FORESTRY_BOOK_NO to logDelivery.forestryBookNo,
                    SUPPLIER to supplierRepository.findById(logDelivery.supplierId)!!.name,
                    RECEIVED_DATE to receivedDateFormat.format(log.receivedAt)
                )
            )
        }
    }
}