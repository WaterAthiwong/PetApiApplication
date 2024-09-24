package com.champaca.inventorydata.fg.flooring.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.FinishedGoodSticker
import com.champaca.inventorydata.databasetable.dao.FinishedGoodStickerDao
import com.champaca.inventorydata.fg.flooring.request.SwitchStickerBatchRequest
import com.champaca.inventorydata.fg.flooring.response.SwitchStickerBatchResponse
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class SwitchStickerBatchUseCase(
    val dataSource: DataSource
) {
    val logger = LoggerFactory.getLogger(SwitchStickerBatchUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: SwitchStickerBatchRequest): SwitchStickerBatchResponse {
        Database.connect(dataSource)

        lateinit var stickers: List<FinishedGoodStickerDao>
        transaction {
            addLogger(exposedLogger)

            stickers = FinishedGoodStickerDao.find {
                (FinishedGoodSticker.code inList request.codes) and (FinishedGoodSticker.status eq "A")
            }.toList()
            val now = LocalDateTime.now()
            stickers.forEach {
                it.batchId = request.batchId
                it.updatedAt = now
                it.printedAt = null
            }
        }

        return SwitchStickerBatchResponse.Success(stickers.map { it.code })
    }
}