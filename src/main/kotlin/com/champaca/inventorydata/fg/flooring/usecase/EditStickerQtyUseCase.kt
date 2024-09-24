package com.champaca.inventorydata.fg.flooring.usecase

import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.FinishedGoodSticker
import com.champaca.inventorydata.databasetable.FinishedGoodStickerHasSku
import com.champaca.inventorydata.databasetable.dao.FinishedGoodStickerDao
import com.champaca.inventorydata.fg.flooring.request.EditStickerQtyRequest
import com.champaca.inventorydata.fg.flooring.response.EditStickerQtyResponse
import com.champaca.inventorydata.pile.PileError
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class EditStickerQtyUseCase(
    val dataSource: DataSource
) {
    val logger = LoggerFactory.getLogger(EditStickerQtyUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: EditStickerQtyRequest): EditStickerQtyResponse {
        Database.connect(dataSource)

        var error = PileError.NONE
        var errorMessage = ""
        transaction {
            addLogger(exposedLogger)

            val sticker = FinishedGoodStickerDao.find { (FinishedGoodSticker.id eq request.stickerId) and (FinishedGoodSticker.status eq "A") }
                .firstOrNull()
            if (sticker == null) {
                error = PileError.PILE_NOT_FOUND
                errorMessage = "Sticker not found"
                logger.warn("Sticker ${request.stickerId} not found")
                return@transaction
            }

            FinishedGoodStickerHasSku.update({ FinishedGoodStickerHasSku.stickerId eq request.stickerId }) {
                it[qty] = request.qty.toBigDecimal()
            }
            FinishedGoodSticker.update({ FinishedGoodSticker.id eq request.stickerId }) {
                it[updatedAt] = LocalDateTime.now()
            }
        }

        if (error != PileError.NONE) {
            return EditStickerQtyResponse.Failure(error, errorMessage)
        }

        return EditStickerQtyResponse.Success(true)
    }
}