package com.champaca.inventorydata.fg.flooring.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.FinishedGoodSticker
import com.champaca.inventorydata.fg.flooring.request.RecordStickerPrintedRequest
import com.champaca.inventorydata.fg.flooring.response.RecordStickerPrintedResponse
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class RecordStickerPrintedUseCase(
    val dataSource: DataSource
) {
    val logger = LoggerFactory.getLogger(RecordStickerPrintedUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: RecordStickerPrintedRequest): RecordStickerPrintedResponse {
        Database.connect(dataSource)

        transaction {
            addLogger(exposedLogger)

            val now = LocalDateTime.now()
            FinishedGoodSticker.update({ (FinishedGoodSticker.code inList request.codes) and (FinishedGoodSticker.status eq "A") }) {
                it[printedAt] = now
            }
        }

        return RecordStickerPrintedResponse.Success(request.codes.size)
    }
}