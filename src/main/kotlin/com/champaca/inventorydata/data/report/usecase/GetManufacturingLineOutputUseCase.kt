package com.champaca.inventorydata.data.report.usecase

import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.data.report.request.GetManufacturingLineOutputRequest
import com.champaca.inventorydata.data.report.response.PileTransactionEntry
import com.champaca.inventorydata.data.report.response.ProductionOutputResponse
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class GetManufacturingLineOutputUseCase(
    val dataSource: DataSource
) {
    companion object {
        val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    }

    val logger = LoggerFactory.getLogger(GetManufacturingLineOutputUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)
    fun execute(request: GetManufacturingLineOutputRequest): ProductionOutputResponse {
        Database.connect(dataSource)
        var results = listOf<PileTransactionEntry>()
        transaction {
            addLogger(exposedLogger)

            results = getOutput(request)
        }
        return ProductionOutputResponse(
            transactions = results,
            totalPiles = results.map { it.pileCode }.distinct().size,
            totalPieces = results.sumOf { it.qty },
            totalAreaM2 = results.sumOf { it.areaM2 }
        )
    }

    private fun getOutput(request: GetManufacturingLineOutputRequest): List<PileTransactionEntry> {
        val fromProductionDate = LocalDate.parse(request.fromProductionDate, DATE_FORMAT)
        val toProductionDate = LocalDate.parse(request.toProductionDate, DATE_FORMAT)
        val joins = GoodMovement.join(GmItem, JoinType.INNER) { (GoodMovement.id eq GmItem.goodMovementId) }
            .join(LotNo, JoinType.INNER) { (GmItem.lotNoId eq LotNo.id) }
            .join(Sku, JoinType.INNER) { (GmItem.skuId eq Sku.id) }
            .join(PileHasLotNo, JoinType.INNER) { (LotNo.id eq PileHasLotNo.lotNoId) }
            .join(Pile, JoinType.INNER) { (PileHasLotNo.pileId eq Pile.id) }
            .join(ManufacturingLine, JoinType.INNER) { (GoodMovement.manufacturingLineId eq ManufacturingLine.id) }
        val query = joins.select(
            GoodMovement.type,
            Pile.type,
            Pile.code,
            Pile.id,
            Sku.matCode,
            Sku.id,
            Sku.name,
            Sku.thickness,
            Sku.width,
            Sku.length,
            Sku.lengthUom,
            Sku.fsc,
            Sku.grade,
            Sku.species,
            Sku.skuGroupId,
            LotNo.id,
            Pile.orderNo,
            GoodMovement.jobNo,
            GmItem.qty,
            Sku.volumnFt3,
            Sku.volumnM3,
            Sku.areaM2,
            Pile.remark,
            GoodMovement.code,
            ManufacturingLine.id,
            ManufacturingLine.name,
            GoodMovement.productionDate,
            GoodMovement.orderNo,
            GmItem.createdAt,
            Pile.type
        )
            .where { (Pile.status eq "A") and (LotNo.status eq "A") and (GmItem.status eq "A") and
                    (GoodMovement.departmentId eq request.departmentId) and
                    (GmItem.createdAt.date() greaterEq fromProductionDate) and (GmItem.createdAt.date() lessEq toProductionDate)
            }
            .orderBy(ManufacturingLine.name to SortOrder.ASC, GmItem.createdAt to SortOrder.ASC)

        if (request.manufacturingLineIds.isNotEmpty()) {
            query.andWhere { ManufacturingLine.id inList request.manufacturingLineIds }
        }

        if (request.pick && request.receive) {
            // Do nothing
        } else if (request.pick) {
            query.andWhere { GoodMovement.type eq GoodMovementType.PICKING_ORDER.wmsName }
        } else if (request.receive) {
            query.andWhere { GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName }
        }

        return query.map {
            val goodMovementType = when(it[GoodMovement.type]) {
                GoodMovementType.GOODS_RECEIPT.wmsName -> "ออกจากการผลิต"
                GoodMovementType.PICKING_ORDER.wmsName -> "เบิกเข้าผลิต"
                else -> ""
            }
            PileTransactionEntry(
                type = goodMovementType,
                pileCode = it[Pile.code],
                pileId = it[Pile.id].value,
                matCode = it[Sku.matCode],
                skuId = it[Sku.id].value,
                skuName = it[Sku.name],
                thickness = it[Sku.thickness],
                width = it[Sku.width],
                length = it[Sku.length],
                lengthUom = it[Sku.lengthUom],
                fsc = it[Sku.fsc],
                grade = it[Sku.grade] ?: "",
                species = it[Sku.species],
                skuGroupId = it[Sku.skuGroupId],
                lotId = it[LotNo.id].value,
                orderNo = it[Pile.orderNo],
                jobNo = it[GoodMovement.jobNo],
                qty = it[GmItem.qty],
                volumnFt3 = it[Sku.volumnFt3],
                areaM2 = it[Sku.areaM2] ?: BigDecimal.ZERO,
                remark = it[Pile.remark],
                date = it[GmItem.createdAt].toLocalDate(),
                goodMovementCode = it[GoodMovement.code],
                manufacturingLineId = it[ManufacturingLine.id].value,
                manufacturingLine = it[ManufacturingLine.name],
                supplier = "",
                department = "",
                lotRefCode = "",
                productionDate = it[GoodMovement.productionDate],
                poNo = "",
                lotNo = "",
                invoiceNo = "",
                goodMovementOrderNo = it[GoodMovement.orderNo],
                goodMovementExtraAttributes = null,
                pileExtraAttributes = null,
                transactionAt = it[GmItem.createdAt],
                pileType = it[Pile.type]
            ).apply {
                transtionAtStr = it[GmItem.createdAt].format(DATE_TIME_FORMAT)
            }
        }
    }
}