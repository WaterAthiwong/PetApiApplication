package com.champaca.inventorydata.fg.flooring.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.fg.flooring.model.StickerData
import com.champaca.inventorydata.fg.flooring.request.GetStickerDetailsRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class GetStickerDetialsUseCase(
    val dataSource: DataSource
) {
    companion object {
        val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    }

    val logger = LoggerFactory.getLogger(GetStickerDetialsUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: GetStickerDetailsRequest): List<StickerData> {
        Database.connect(dataSource)

        var results = listOf<StickerData>()
        transaction {
            addLogger(exposedLogger)
            results = getStickers(request)
        }

        return results
    }

    private fun getStickers(request: GetStickerDetailsRequest): List<StickerData> {
        val joins = FinishedGoodStickerBatch.join(FinishedGoodSticker, JoinType.INNER) { FinishedGoodStickerBatch.id eq FinishedGoodSticker.batchId }
            .join(FinishedGoodStickerHasSku, JoinType.INNER) { FinishedGoodStickerHasSku.stickerId eq FinishedGoodSticker.id }
            .join(Sku, JoinType.INNER) { FinishedGoodStickerHasSku.skuId eq Sku.id }
            .join(SkuGroup, JoinType.INNER) { Sku.skuGroupId eq SkuGroup.id }
            .join(Supplier, JoinType.INNER) { FinishedGoodStickerBatch.suppilerId eq Supplier.id }
            .join(Pile, JoinType.LEFT) { Pile.id eq FinishedGoodSticker.pileId }
        val query = joins.select(FinishedGoodStickerBatch.code, FinishedGoodSticker.code, Supplier.name, FinishedGoodStickerBatch.salesOrderNo,
            FinishedGoodStickerBatch.salesOrderLineNo, Sku.species, Sku.extraAttributes, FinishedGoodStickerBatch.remark, FinishedGoodStickerBatch.remark2,
            FinishedGoodStickerBatch.productionDate, Sku.extraAttributes, Sku.matCode, Sku.thickness, Sku.width, Sku.length, FinishedGoodStickerHasSku.qty,
            FinishedGoodSticker.printedAt, Pile.createdAt, SkuGroup.name, FinishedGoodSticker.id)
            .where {
                (FinishedGoodStickerBatch.id eq request.batchId) and (FinishedGoodSticker.status eq "A")
            }
            .orderBy(FinishedGoodSticker.code)

        if (request.rangeFrom != null && request.rangeTo != null) {
            var rangeFrom = request.rangeFrom - 1
            var rangeTo = request.rangeTo - rangeFrom
            query.limit(rangeTo, rangeFrom.toLong())
        }
        if (request.codes.isNotEmpty()) {
            query.andWhere { FinishedGoodSticker.code inList request.codes }
        }
        return query.toList().groupBy { it[FinishedGoodSticker.code] }
            .mapValues { (_, value) ->
                val first = value.first()
                val extAttr = first[Sku.extraAttributes] ?: emptyMap()
                val items = value.map {
                    StickerData.StickerItem(
                        matCode = it[Sku.matCode],
                        extraAttributes = it[Sku.extraAttributes],
                        thickness = it[Sku.thickness].setScale(2, RoundingMode.HALF_UP),
                        width = it[Sku.width].setScale(2, RoundingMode.HALF_UP),
                        height = it[Sku.length].setScale(2, RoundingMode.HALF_UP),
                        qty = it[FinishedGoodStickerHasSku.qty].setScale(0, RoundingMode.HALF_UP),
                    )
                }
                val printedAt = if (first[FinishedGoodSticker.printedAt] != null) {
                    DATETIME_FORMAT.format(first[FinishedGoodSticker.printedAt])
                } else {
                    ""
                }
                val registeredAt = if (first[Pile.createdAt] != null) {
                    DATETIME_FORMAT.format(first[Pile.createdAt])
                } else {
                    ""
                }
                StickerData(
                    id = first[FinishedGoodSticker.id].value,
                    code = first[FinishedGoodSticker.code],
                    batchCode = first[FinishedGoodStickerBatch.code],
                    customer = first[Supplier.name],
                    salesOrderNo = first[FinishedGoodStickerBatch.salesOrderNo],
                    salesOrderLineNo = first[FinishedGoodStickerBatch.salesOrderLineNo],
                    top = first[Sku.species],
                    type = first[SkuGroup.name],
                    color = extAttr["color"] ?: "",
                    remark = first[FinishedGoodStickerBatch.remark] ?: "",
                    remark2 = first[FinishedGoodStickerBatch.remark2] ?: "",
                    date = DATE_FORMAT.format(first[FinishedGoodStickerBatch.productionDate]),
                    items = items,
                    printedAt = printedAt,
                    registeredAt = registeredAt,
                    canEdit = first[Pile.createdAt] == null
                )
            }.values.toList()
    }
}