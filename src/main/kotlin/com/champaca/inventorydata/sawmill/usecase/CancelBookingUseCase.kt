package com.champaca.inventorydata.sawmill.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.BookedLog
import com.champaca.inventorydata.databasetable.LogTransaction
import com.champaca.inventorydata.databasetable.LotNo
import com.champaca.inventorydata.databasetable.dao.LogTransactionDao
import com.champaca.inventorydata.databasetable.dao.LotNoDao
import com.champaca.inventorydata.sawmill.SawMillError
import com.champaca.inventorydata.sawmill.request.CancelBookingRequest
import com.champaca.inventorydata.sawmill.response.CancelBookingResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class CancelBookingUseCase(
    val dataSource: DataSource
) {
    val logger = LoggerFactory.getLogger(CancelBookingUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, userId: String, request: CancelBookingRequest): CancelBookingResponse {
        Database.connect(dataSource)

        var error = SawMillError.NONE
        var errorMessage = ""
        var lotNo: LotNoDao? = null
        transaction {
            addLogger(exposedLogger)
            lotNo = LotNoDao.find { (LotNo.refCode eq request.barcode) and (LotNo.status eq "A") }.firstOrNull()
            if (lotNo == null) {
                error = SawMillError.BARCODE_NOT_FOUND
                return@transaction
            }

            BookedLog.update({ BookedLog.lotNoId eq lotNo!!.id }) {
                it[status] = "D"
            }

            LogTransaction.insert {
                it[LogTransaction.lotNoId] = lotNo!!.id.value
                it[LogTransaction.type] = LogTransactionDao.CANCEL_BOOKING
                it[LogTransaction.userId] = userId.toInt()
                it[LogTransaction.remark] = request.reason
                it[LogTransaction.createdAt] = LocalDateTime.now()
            }
        }

        return if (error == SawMillError.NONE) {
            CancelBookingResponse.Success(lotNo!!.id.value)
        } else {
            CancelBookingResponse.Failure(error, errorMessage)
        }
    }
}