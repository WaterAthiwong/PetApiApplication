package com.champaca.inventorydata.fg.flooring.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.FinishedGoodSticker
import com.champaca.inventorydata.databasetable.dao.FinishedGoodStickerDao
import com.champaca.inventorydata.fg.flooring.request.GetStickerDetailsRequest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class CalculateStickerRangeUseCase(
    val dataSource: DataSource
) {
    val logger = LoggerFactory.getLogger(CalculateStickerRangeUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: GetStickerDetailsRequest): List<String> {
        Database.connect(dataSource)

        var results = listOf<String>()
        transaction {
            addLogger(exposedLogger)
            results = getStickerCodes(request)
        }
        return results
    }

    private fun getStickerCodes(request: GetStickerDetailsRequest): List<String> {
        val query = FinishedGoodSticker.select(FinishedGoodSticker.code).where {
            (FinishedGoodSticker.batchId eq request.batchId) and
            (FinishedGoodSticker.status eq "A")
        }
        val codes = query.map { it[FinishedGoodSticker.code] }

        if (request.rangeFrom == null && request.rangeTo == null) {
            return codes
        }

        if (request.rangeFrom == null) {
            return codes.take(request.rangeTo!!)
        }

        if (request.rangeTo == null) {
            return codes.drop(request.rangeFrom)
        }
        return codes.subList(request.rangeFrom, request.rangeTo)
    }
}