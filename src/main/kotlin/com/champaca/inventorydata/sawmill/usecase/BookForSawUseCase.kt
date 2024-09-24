package com.champaca.inventorydata.sawmill.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.databasetable.BookedLog
import com.champaca.inventorydata.databasetable.LogTransaction
import com.champaca.inventorydata.databasetable.LotNo
import com.champaca.inventorydata.databasetable.dao.GoodMovementDao
import com.champaca.inventorydata.databasetable.dao.LogTransactionDao
import com.champaca.inventorydata.databasetable.dao.LotNoDao
import com.champaca.inventorydata.log.model.StoredLog
import com.champaca.inventorydata.pile.request.RelocatePileRequest
import com.champaca.inventorydata.pile.response.RelocatePileResponse
import com.champaca.inventorydata.pile.usecase.RelocatePileUseCase
import com.champaca.inventorydata.sawmill.SawMillError
import com.champaca.inventorydata.sawmill.SawMillService
import com.champaca.inventorydata.sawmill.request.SawRequest
import com.champaca.inventorydata.sawmill.response.SawResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class BookForSawUseCase(
    val dataSource: DataSource,
    val itemLockService: ItemLockService,
    val sawMillService: SawMillService,
    val relocatePileUseCase: RelocatePileUseCase
) {
    val logger = LoggerFactory.getLogger(BookForSawUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, userId: String, request: SawRequest): SawResponse {
        lateinit var storedLog: StoredLog
        var error = SawMillError.NONE
        var errorMessage = ""
        var reservedLogCount = 0
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

                val hasBeenBooked = hasLogBeenBooked(lotNo)
                if (hasBeenBooked) {
                    error = SawMillError.LOG_HAS_BEEN_BOOKED
                    logger.warn("Barcode: ${request.barcode}. Log has been already booked.")
                    return@transaction
                }


                val goodMovement = GoodMovementDao.findById(request.goodMovementId)!!
                if (goodMovement.approveUserId != null) {
                    error = SawMillError.GOOD_MOVEMENT_HAS_BEEN_APPROVED
                    logger.warn("Barcode: ${request.barcode}. Good movement has been approved.")
                    return@transaction
                }

                val now = LocalDateTime.now()
                BookedLog.upsert {
                    it[BookedLog.lotNoId] = lotNo.id.value
                    it[BookedLog.goodMovementId] = request.goodMovementId
                    it[BookedLog.status] = "A"
                }

                LogTransaction.insert {
                    it[LogTransaction.lotNoId] = lotNo.id.value
                    it[LogTransaction.toGoodMovementId] = request.goodMovementId
                    it[LogTransaction.userId] = userId.toInt()
                    it[LogTransaction.type] = LogTransactionDao.BOOK
                    it[LogTransaction.createdAt] = now
                }

                storedLog = sawMillService.getStoredLog(lotNo)
                reservedLogCount = findBookedForGoodMovement(request.goodMovementId)
            }

            if (error != SawMillError.NONE) {
                return SawResponse.Failure(errorType = error, errorMessage = errorMessage)
            }

            if (!request.location.isNullOrEmpty()) {
                // อันนี้เป็น requirement ใหม่จากทางน้ำ (จิดาภา พึ่งกุศล) ว่า ถ้าจองขึ้นเลื่อยแล้วแปลว่า มันมีการย้ายไม้ซุงจากลานดินไปลานปูนแล้วด้วย ก็ให้ทำกาย้ายตำแหน่งด้วยเลย
                val relocateResponse = relocatePileUseCase.execute(sessionId, userId, RelocatePileRequest("${request.barcode}\n${request.location}"))
                if (relocateResponse is RelocatePileResponse.Failure) {
                    logger.warn("Barcode: ${request.barcode}. Relocation failed.")
                    return SawResponse.Failure(errorType = SawMillError.WMS_VALIDATION_ERROR, errorMessage = relocateResponse.errorMessage ?: "")
                }
            }

            return SawResponse.Success(storedLog = storedLog, itemCount = reservedLogCount)
        } finally {
            // unlock this refCode when the sawing (pick the log) is done.
            itemLockService.unlock(request.barcode)
        }
    }

    private fun findBookedForGoodMovement(goodMovementId: Int): Int {
        val query = BookedLog.select(BookedLog.lotNoId.count())
            .where { (BookedLog.goodMovementId eq goodMovementId) and (BookedLog.status eq "A")}
        return query.map { resultRow -> resultRow[BookedLog.lotNoId.count()] }.first().toInt() ?: 0
    }

    private fun hasLogBeenBooked(lotNo: LotNoDao): Boolean {
        val query = BookedLog.selectAll().where { (BookedLog.lotNoId eq lotNo.id) and (BookedLog.status eq "A") }
        return query.count() > 0
    }
}