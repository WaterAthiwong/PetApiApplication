package com.champaca.inventorydata.fg.flooring.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.databasetable.FinishedGoodSticker
import com.champaca.inventorydata.databasetable.FinishedGoodStickerBatch
import com.champaca.inventorydata.databasetable.FinishedGoodStickerHasSku
import com.champaca.inventorydata.fg.flooring.request.CreateStickerBatchRequest
import com.champaca.inventorydata.fg.flooring.response.CreateStickerBatchResponse
import com.champaca.inventorydata.masterdata.config.ConfigRepository
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.utils.DateTimeUtil
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class CreateStickerBatchUseCase(
    val dataSource: DataSource,
    val itemLockService: ItemLockService,
    val dateTimeUtil: DateTimeUtil,
    val configRepository: ConfigRepository,
    val pileService: PileService
) {

    companion object {
        val INPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    val logger = LoggerFactory.getLogger(CreateStickerBatchUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: CreateStickerBatchRequest): CreateStickerBatchResponse {
        val lockStr = "FgSticker${request.departmentPrefix}"
        try {
            itemLockService.lock(lockStr)
            Database.connect(dataSource)

            lateinit var newBatchCode: String
            var newBatchId = -1
            transaction {
                addLogger(exposedLogger)

                newBatchCode = newBatchCode(request.departmentPrefix)
                newBatchId = FinishedGoodStickerBatch.insertAndGetId {
                    it[FinishedGoodStickerBatch.suppilerId] = request.customerId
                    it[FinishedGoodStickerBatch.code] = newBatchCode
                    it[FinishedGoodStickerBatch.salesOrderNo] = request.salesOrderNo
                    it[FinishedGoodStickerBatch.salesOrderLineNo] = request.salesOrderLineNo
                    it[FinishedGoodStickerBatch.format] = request.format
                    it[FinishedGoodStickerBatch.productionDate] = LocalDate.parse(request.productionDate, INPUT_DATE_FORMAT)
                    it[FinishedGoodStickerBatch.remark] = request.remark
                    it[FinishedGoodStickerBatch.remark2] = request.remark2
                    it[FinishedGoodStickerBatch.createdAt] = LocalDateTime.now()
                    it[FinishedGoodStickerBatch.status] = "A"
                }.value

                if (request.copies > 0) {
                    val startIndex = 1
                    val boxCodes = List(request.copies) { "${newBatchCode}${(startIndex + it).toString().padStart(3, '0')}" } // FL2407001001, FL2407001002, ...
                    var stickers = insertStickers(newBatchId, boxCodes, false)
                    val stickerSkus = mutableListOf<Triple<Int, Int, BigDecimal>>()
                    stickers.forEach { sticker ->
                        request.items.forEach { item ->
                            stickerSkus.add(Triple(sticker[FinishedGoodSticker.id].value, item.skuId, item.qty))
                        }
                    }
                    insertStickerHasSku(stickerSkus)
                }

                if (request.fragmentQtys.isNotEmpty()) {
                    val startIndex = request.copies + 1
                    val boxCodes = List(request.fragmentQtys.size) { "${newBatchCode}${(startIndex + it).toString().padStart(3, '0')}" } // FL2407001004, FL2407001005, ...
                    val stickers = insertStickers(newBatchId, boxCodes, true)
                    val firstItem = request.items.first()
                    val fragmentStickerSkus = stickers.mapIndexed { index, sticker -> Triple(sticker[FinishedGoodSticker.id].value, firstItem.skuId, request.fragmentQtys[index].toBigDecimal()) }
                    insertStickerHasSku(fragmentStickerSkus)
                }
            }

            return CreateStickerBatchResponse.Success(newBatchId, newBatchCode)
        } finally {
            itemLockService.unlock(lockStr)
        }
    }

    private fun newBatchCode(prefix: String): String {
        val monthPrefix = dateTimeUtil.getYearMonthPrefix()
        val batchPrefix = "${prefix}${monthPrefix}" // e.g. CX2403
        val config = configRepository.getOrPut("FgStickerBatch.${batchPrefix}", defaultInt = 0)
        val nextRunning = config.valueInt!! + 1
        val newBatchCode = "${batchPrefix}${(nextRunning).toString().padStart(3, '0')}" // e.g. FL2407001
        config.valueInt = nextRunning
        return newBatchCode
    }

    private fun insertStickers(newBatchId: Int, codes: List<String>, isFragment: Boolean): List<ResultRow> {
        val now = LocalDateTime.now()
        return FinishedGoodSticker.batchInsert(codes) {
            this[FinishedGoodSticker.batchId] = newBatchId
            this[FinishedGoodSticker.code] = it
            this[FinishedGoodSticker.isFragment] = isFragment
            this[FinishedGoodSticker.createdAt] = now
            this[FinishedGoodSticker.updatedAt] = now
            this[FinishedGoodSticker.status] = "A"
        }
    }

    private fun insertStickerHasSku(stickerSkus: List<Triple<Int, Int, BigDecimal>>) {
        FinishedGoodStickerHasSku.batchInsert(stickerSkus) { t ->
            this[FinishedGoodStickerHasSku.stickerId] = t.first
            this[FinishedGoodStickerHasSku.skuId] = t.second
            this[FinishedGoodStickerHasSku.qty] = t.third
            this[FinishedGoodStickerHasSku.status] = "A"
        }
    }
}