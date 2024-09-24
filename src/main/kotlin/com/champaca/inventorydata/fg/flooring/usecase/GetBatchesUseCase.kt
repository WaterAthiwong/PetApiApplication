package com.champaca.inventorydata.fg.flooring.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.FinishedGoodSticker
import com.champaca.inventorydata.databasetable.FinishedGoodStickerBatch
import com.champaca.inventorydata.databasetable.FinishedGoodStickerHasSku
import com.champaca.inventorydata.databasetable.Sku
import com.champaca.inventorydata.fg.flooring.model.BatchData
import com.champaca.inventorydata.fg.flooring.request.GetBatchesRequest
import com.champaca.inventorydata.masterdata.supplier.SupplierRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class GetBatchesUseCase(
    val dataSource: DataSource,
    val supplierRepository: SupplierRepository
) {

    companion object {
        val INPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val OUTPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    }

    val logger = LoggerFactory.getLogger(GetBatchesUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: GetBatchesRequest): List<BatchData> {
        Database.connect(dataSource)

        var results = listOf<BatchData>()
        transaction {
            addLogger(exposedLogger)
            results = getBatches(request)
            results = updateQty(results)
            results = updateRegisteredQty(results)
            results = updateMatCode(results)
        }
        return results
    }

    private fun getBatches(request: GetBatchesRequest): List<BatchData> {
        val query = FinishedGoodStickerBatch.selectAll().where { FinishedGoodStickerBatch.status eq "A" }
            .orderBy(FinishedGoodStickerBatch.id, SortOrder.DESC)

        if (!request.createdDateFrom.isNullOrEmpty()) {
            query.andWhere { FinishedGoodStickerBatch.productionDate greaterEq LocalDate.parse(request.createdDateFrom, INPUT_DATE_FORMAT) }
        }
        if (!request.createdDateTo.isNullOrEmpty()) {
            query.andWhere { FinishedGoodStickerBatch.productionDate lessEq LocalDate.parse(request.createdDateTo, INPUT_DATE_FORMAT) }
        }
        if (!request.salesOrderPattern.isNullOrEmpty()) {
            query.andWhere { FinishedGoodStickerBatch.salesOrderNo like "${request.salesOrderPattern}%" }
        }
        if (request.customerId != null) {
            query.andWhere { FinishedGoodStickerBatch.suppilerId eq request.customerId }
        }

        return query.map {
            val supplierId = it[FinishedGoodStickerBatch.suppilerId]
            BatchData(
                id = it[FinishedGoodStickerBatch.id].value,
                code = it[FinishedGoodStickerBatch.code],
                salesOrderNo = it[FinishedGoodStickerBatch.salesOrderNo],
                salesOrderLineNo = it[FinishedGoodStickerBatch.salesOrderLineNo],
                format = it[FinishedGoodStickerBatch.format],
                customer = supplierRepository.findById(supplierId)!!.name,
                createdAt = OUTPUT_DATE_FORMAT.format(it[FinishedGoodStickerBatch.createdAt]),
                remark = it[FinishedGoodStickerBatch.remark],
                remark2 = it[FinishedGoodStickerBatch.remark2],
            )
        }
    }

    private fun updateQty(batches: List<BatchData>): List<BatchData> {
        val batchIds = batches.map { it.id }
        val query = FinishedGoodSticker.select(FinishedGoodSticker.id.count(), FinishedGoodSticker.batchId)
            .where { (FinishedGoodSticker.status eq "A") and (FinishedGoodSticker.batchId inList batchIds) }
            .groupBy(FinishedGoodSticker.batchId)
        val qtyMap = query.associate { it[FinishedGoodSticker.batchId] to it[FinishedGoodSticker.id.count()] }
        batches.forEach { it.qty = qtyMap[it.id]?.toInt() ?: 0 }
        return batches
    }

    private fun updateRegisteredQty(batches: List<BatchData>): List<BatchData> {
        val batchIds = batches.map { it.id }
        val query = FinishedGoodSticker.select(FinishedGoodSticker.id.count(), FinishedGoodSticker.batchId)
            .where { (FinishedGoodSticker.status eq "A") and (FinishedGoodSticker.batchId inList batchIds) and (FinishedGoodSticker.pileId.isNotNull()) }
            .groupBy(FinishedGoodSticker.batchId)
        val qtyMap = query.associate { it[FinishedGoodSticker.batchId] to it[FinishedGoodSticker.id.count()] }
        batches.forEach { it.registeredQty = qtyMap[it.id]?.toInt() ?: 0 }
        return batches
    }

    private fun updateMatCode(batches: List<BatchData>): List<BatchData> {
        val batchIds = batches.map { it.id }
        val joins = FinishedGoodSticker.join(FinishedGoodStickerHasSku, JoinType.INNER) { FinishedGoodSticker.id eq FinishedGoodStickerHasSku.stickerId }
            .join(Sku, JoinType.INNER) { FinishedGoodStickerHasSku.skuId eq Sku.id }
        val query = joins.select(FinishedGoodSticker.batchId, Sku.matCode)
            .where { (FinishedGoodSticker.status eq "A") and (FinishedGoodSticker.batchId inList batchIds) }
        val matCodeMap = query.associate { it[FinishedGoodSticker.batchId] to it[Sku.matCode] }
        batches.forEach { it.matCode = matCodeMap[it.id] ?: "" }
        return batches
    }
}