package com.champaca.inventorydata.fg.flooring.usecase

import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.FinishedGoodSticker
import com.champaca.inventorydata.databasetable.FinishedGoodStickerBatch
import com.champaca.inventorydata.databasetable.FinishedGoodStickerHasSku
import com.champaca.inventorydata.databasetable.Sku
import com.champaca.inventorydata.databasetable.dao.FinishedGoodStickerBatchDao
import com.champaca.inventorydata.fg.flooring.request.AddStickerToBatchRequest
import com.champaca.inventorydata.fg.flooring.response.AddStickerToBatchResponse
import com.champaca.inventorydata.pile.PileError
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class AddStickerToBatchUseCase(
    val dataSource: DataSource
) {
    val logger = LoggerFactory.getLogger(AddStickerToBatchUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: AddStickerToBatchRequest): AddStickerToBatchResponse {
        Database.connect(dataSource)

        var error = PileError.NONE
        var errorMessage = ""
        transaction {
            addLogger(exposedLogger)

            val batch = FinishedGoodStickerBatchDao.find { (FinishedGoodStickerBatch.id eq request.batchId) and
                    (FinishedGoodStickerBatch.status eq "A") }
                .firstOrNull()
            if (batch == null) {
                error = PileError.PILE_NOT_FOUND
                errorMessage = "Batch not found"
                logger.warn("Batch ${request.batchId} not found")
                return@transaction
            }

            val row = findLastStickerData(request)
            if (row == null) {
                error = PileError.INVALID_REQUEST
                errorMessage = "Sticker data not found"
                logger.warn("Sticker data under batch ${request.batchId} not found")
                return@transaction
            }

            val code = row[FinishedGoodSticker.code]
            val running = code.substring(code.length - 3).toInt()
            val newStickerCode = code.substring(0, code.length - 3) + (running + 1).toString().padStart(3, '0')
            val oldQty = row[FinishedGoodStickerHasSku.qty].toInt()

            val now = LocalDateTime.now()
            val stickerId = FinishedGoodSticker.insertAndGetId {
                it[FinishedGoodSticker.batchId] = request.batchId
                it[FinishedGoodSticker.code] = newStickerCode
                it[FinishedGoodSticker.isFragment] = request.qty < oldQty
                it[FinishedGoodSticker.createdAt] = now
                it[FinishedGoodSticker.updatedAt] = now
                it[FinishedGoodSticker.status] = "A"
            }

            FinishedGoodStickerHasSku.insert {
                it[FinishedGoodStickerHasSku.stickerId] = stickerId.value
                it[FinishedGoodStickerHasSku.skuId] = row[Sku.id]
                it[FinishedGoodStickerHasSku.qty] = request.qty.toBigDecimal()
                it[FinishedGoodStickerHasSku.status] = "A"
            }
        }

        if (error != PileError.NONE) {
            return AddStickerToBatchResponse.Failure(error, errorMessage)
        }

        return AddStickerToBatchResponse.Success(true)
    }

    private fun findLastStickerData(request: AddStickerToBatchRequest): ResultRow? {
        val joins = FinishedGoodSticker.join(FinishedGoodStickerHasSku, JoinType.INNER) { FinishedGoodSticker.id eq FinishedGoodStickerHasSku.stickerId }
            .join(Sku, JoinType.INNER) { FinishedGoodStickerHasSku.skuId eq Sku.id }
        val query = joins.select(FinishedGoodSticker.code, FinishedGoodStickerHasSku.qty, FinishedGoodSticker.isFragment, Sku.id)
            .where { (FinishedGoodSticker.batchId eq request.batchId) and (FinishedGoodStickerBatch.status eq "A") and
                    (FinishedGoodSticker.status eq "A") and (Sku.status eq "A") and (FinishedGoodSticker.isFragment eq false) }
            .orderBy(FinishedGoodSticker.code to SortOrder.DESC)
            .limit(1)
        return query.firstOrNull()
    }
}