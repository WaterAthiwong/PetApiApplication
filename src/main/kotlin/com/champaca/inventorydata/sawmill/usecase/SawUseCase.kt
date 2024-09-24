package com.champaca.inventorydata.sawmill.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.GmItem
import com.champaca.inventorydata.databasetable.GoodMovement
import com.champaca.inventorydata.databasetable.LogTransaction
import com.champaca.inventorydata.databasetable.LotNo
import com.champaca.inventorydata.databasetable.dao.LogTransactionDao
import com.champaca.inventorydata.databasetable.dao.LotNoDao
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.log.model.StoredLog
import com.champaca.inventorydata.sawmill.SawMillError
import com.champaca.inventorydata.sawmill.SawMillService
import com.champaca.inventorydata.sawmill.request.SawRequest
import com.champaca.inventorydata.sawmill.response.SawResponse
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class SawUseCase(
    val dataSource: DataSource,
    val wmsService: WmsService,
    val itemLockService: ItemLockService,
    val sawMillService: SawMillService
) {
    val logger = LoggerFactory.getLogger(SawUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, userId: String, request: SawRequest): SawResponse {
        lateinit var storedLog: StoredLog
        var error = SawMillError.NONE

        try {
            // Lock this refCode, so other requests that may work on this refCode have to wait until the current
            // request is done.
            itemLockService.lock(request.barcode)

            Database.connect(dataSource)
            transaction {
                addLogger(exposedLogger)
                val lotNo = LotNoDao.find { (LotNo.refCode eq request.barcode) and (LotNo.status eq "A") }.firstOrNull()
                if (lotNo == null) {
                    error = SawMillError.BARCODE_NOT_FOUND
                    logger.warn("Barcode: ${request.barcode}. Barcode not found.")
                    return@transaction
                }

                val hasBeenPicked = sawMillService.hasLogBeenPicked(lotNo)
                if (hasBeenPicked) {
                    error = SawMillError.LOG_HAS_BEEN_USED
                    logger.warn("Barcode: ${request.barcode}. Log has been already picked.")
                    return@transaction
                }

                storedLog = sawMillService.getStoredLog(lotNo)
            }

            if (error != SawMillError.NONE) {
                return SawResponse.Failure(errorType = error)
            }

            val result = wmsService.pickGmItem(
                sessionId = sessionId,
                goodMovementType = GoodMovementType.PICKING_ORDER,
                goodMovementId = request.goodMovementId,
                skuId = storedLog.skuId,
                storeLocationId = storedLog.storeLocationId,
                manufacturingLineId = request.manufacturingLineId,
                lotNo = "${storedLog.logNo} | ${storedLog.matCode} | ${request.barcode} | ${storedLog.location}",
                lotNoId = storedLog.lotNoId,
                qty = 1.toBigDecimal()
            )

            return if (result is ResultOf.Success) {
                var itemCount = 0
                transaction {
                    addLogger(exposedLogger)

                    LogTransaction.insert {
                        it[LogTransaction.lotNoId] = storedLog.lotNoId
                        it[LogTransaction.fromGoodMovementId] = storedLog.goodMovementId
                        it[LogTransaction.toGoodMovementId] = request.goodMovementId
                        it[LogTransaction.userId] = userId.toInt()
                        it[LogTransaction.type] = LogTransactionDao.SAW
                        it[LogTransaction.createdAt] = LocalDateTime.now()
                    }

                    itemCount = getItemCount(request.goodMovementId)
                }
                SawResponse.Success(storedLog = storedLog, itemCount = itemCount)
            } else {
                val errorResult = result as ResultOf.Failure
                logger.warn("Barcode: ${request.barcode}. WMS validation error: ${errorResult.message}")
                SawResponse.Failure(errorType = SawMillError.WMS_VALIDATION_ERROR, errorMessage = errorResult.message!!)
            }
        } finally {
            // unlock this refCode when the sawing (pick the log) is done.
            itemLockService.unlock(request.barcode)
        }
    }

    private fun getItemCount(goodMovementId: Int): Int {
        val joins = GmItem.join(GoodMovement, JoinType.INNER) { GmItem.goodMovementId eq GoodMovement.id }
        val query = joins.select(GmItem.qty.sum())
            .where { (GmItem.status eq "A") and (GoodMovement.id eq goodMovementId) }
        return query.map { resultRow -> resultRow[GmItem.qty.sum()]!!.toInt() }.firstOrNull() ?: 0
    }
}