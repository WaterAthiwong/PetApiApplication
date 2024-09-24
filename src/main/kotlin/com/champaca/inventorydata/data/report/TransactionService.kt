package com.champaca.inventorydata.data.report

import com.champaca.inventorydata.data.report.request.GetPileTransactionRequest
import com.champaca.inventorydata.data.report.response.PileTransactionEntry
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class TransactionService {

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getPileTransaction(request: GetPileTransactionRequest, isPick: Boolean): List<PileTransactionEntry> {
        val gmType = if (isPick) GoodMovementType.PICKING_ORDER.wmsName else GoodMovementType.GOODS_RECEIPT.wmsName
        val originGoodMovement = GoodMovement.alias("originGoodMovement")

        var joins = GoodMovement.join(GmItem, JoinType.INNER) { GoodMovement.id eq GmItem.goodMovementId }
            .join(LotNo, JoinType.INNER) { LotNo.id eq GmItem.lotNoId }
            .join(PileHasLotNo, JoinType.INNER) { LotNo.id eq PileHasLotNo.lotNoId }
            .join(Pile, JoinType.INNER) { Pile.id eq PileHasLotNo.pileId }
            .join(ManufacturingLine, JoinType.LEFT) { ManufacturingLine.id eq GoodMovement.manufacturingLineId }
            .join(ProcessType, JoinType.LEFT) { ProcessType.id eq ManufacturingLine.processTypeId }
            .join(Sku, JoinType.INNER) { Sku.id eq GmItem.skuId }
            .join(originGoodMovement, JoinType.LEFT) { originGoodMovement[GoodMovement.id] eq Pile.originGoodMovementId }
            .join(Supplier, JoinType.LEFT) { Supplier.id eq originGoodMovement[GoodMovement.supplierId] }
            .join(Department, JoinType.INNER) { Department.id eq GoodMovement.departmentId }

        val query = joins.select(
            GoodMovement.type,
            Pile.id,
            Pile.code,
            Sku.matCode,
            GmItem.skuId,
            Sku.name,
            Sku.thickness,
            Sku.width,
            Sku.length,
            Sku.lengthUom,
            Sku.fsc,
            Sku.grade,
            Sku.species,
            Sku.skuGroupId,
            GmItem.lotNoId,
            Pile.orderNo,
            GoodMovement.jobNo,
            GmItem.qty,
            Sku.volumnFt3,
            Sku.areaM2,
            Pile.remark,
            GoodMovement.productionDate,
            GoodMovement.code,
            ManufacturingLine.id,
            ManufacturingLine.name,
            Supplier.name,
            Department.name,
            LotNo.refCode,
            GoodMovement.productionDate,
            originGoodMovement[GoodMovement.poNo],
            originGoodMovement[GoodMovement.lotNo],
            originGoodMovement[GoodMovement.invoiceNo],
            originGoodMovement[GoodMovement.orderNo],
            GoodMovement.extraAttributes,
            Pile.extraAttributes,
            GmItem.createdAt,
            Pile.type
        )
            .where { (LotNo.status eq "A") and (Pile.status eq "A") and (GmItem.status eq "A") and
                    (GoodMovement.type eq gmType) }

        if (request.departmentId != null && request.departmentId > 0) {
            query.andWhere {
                GoodMovement.departmentId eq request.departmentId
            }
        }

        if (!request.code.isNullOrEmpty()) {
            query.andWhere {
                GoodMovement.code eq request.code
            }
        }

        if (!request.fromProductionDate.isNullOrEmpty()) {
            query.andWhere {
                GoodMovement.productionDate greaterEq LocalDate.parse(request.fromProductionDate, dateFormatter)
            }
        }
        if (!request.toProductionDate.isNullOrEmpty()) {
            query.andWhere {
                GoodMovement.productionDate lessEq LocalDate.parse(request.toProductionDate, dateFormatter)
            }
        }

        if (!request.fromTransactionDate.isNullOrEmpty()) {
            query.andWhere {
                GmItem.createdAt.date() greaterEq LocalDate.parse(request.fromTransactionDate, dateFormatter)
            }
        }
        if (!request.toTransactionDate.isNullOrEmpty()) {
            query.andWhere {
                GmItem.createdAt.date() lessEq LocalDate.parse(request.toTransactionDate, dateFormatter)
            }
        }

        return query.map { resultRow ->
            PileTransactionEntry(
                type = resultRow[GoodMovement.type],
                pileCode = resultRow[Pile.code],
                pileId = resultRow[Pile.id].value,
                matCode = resultRow[Sku.matCode],
                skuId = resultRow[GmItem.skuId],
                skuName = resultRow[Sku.name],
                thickness = resultRow[Sku.thickness],
                width = resultRow[Sku.width],
                length = resultRow[Sku.length],
                lengthUom = resultRow[Sku.lengthUom],
                fsc = resultRow[Sku.fsc],
                grade = resultRow[Sku.grade] ?: "",
                species = resultRow[Sku.species],
                skuGroupId = resultRow[Sku.skuGroupId],
                lotId = resultRow[GmItem.lotNoId],
                orderNo = resultRow[Pile.orderNo],
                jobNo = resultRow[GoodMovement.jobNo],
                qty = resultRow[GmItem.qty],
                volumnFt3 = resultRow[Sku.volumnFt3],
                areaM2 = resultRow[Sku.areaM2] ?: BigDecimal.ZERO,
                remark = resultRow[Pile.remark],
                date = resultRow[GoodMovement.productionDate],
                goodMovementCode = resultRow[GoodMovement.code],
                manufacturingLineId = resultRow[ManufacturingLine.id]?.value,
                manufacturingLine = resultRow[ManufacturingLine.name],
                supplier = resultRow[Supplier.name],
                department = resultRow[Department.name],
                lotRefCode = resultRow[LotNo.refCode],
                productionDate = resultRow[GoodMovement.productionDate],
                poNo = resultRow[originGoodMovement[GoodMovement.poNo]],
                lotNo = resultRow[originGoodMovement[GoodMovement.lotNo]],
                invoiceNo = resultRow[originGoodMovement[GoodMovement.invoiceNo]],
                goodMovementOrderNo = resultRow[originGoodMovement[GoodMovement.orderNo]],
                goodMovementExtraAttributes = resultRow[GoodMovement.extraAttributes],
                pileExtraAttributes = resultRow[Pile.extraAttributes],
                transactionAt = resultRow[GmItem.createdAt],
                pileType = resultRow[Pile.type]
            )
        }
    }

    fun getLogTransaction(request: GetPileTransactionRequest): List<ResultRow> {
        var joins = GoodMovement.join(GmItem, JoinType.INNER) { GoodMovement.id eq GmItem.goodMovementId }
            .join(LotNo, JoinType.INNER) { LotNo.id eq GmItem.lotNoId }
            .join(Sku, JoinType.INNER) { Sku.id eq GmItem.skuId }
            .join(Supplier, JoinType.LEFT) { Supplier.id eq GoodMovement.supplierId }
            .join(ManufacturingLine, JoinType.LEFT) { ManufacturingLine.id eq GoodMovement.manufacturingLineId }

        val query = joins.select(
            Sku.circumference,
            Sku.length,
            Sku.fsc,
            Sku.species,
            LotNo.extraAttributes,
            GoodMovement.jobNo,
            GoodMovement.manufacturingLineId,
            ManufacturingLine.name,
            Supplier.name,
            GoodMovement.productionDate,
            GoodMovement.poNo,
            GoodMovement.lotNo,
            GoodMovement.invoiceNo
        )
            .where { (LotNo.status eq "A") and (GoodMovement.type eq GoodMovementType.PICKING_ORDER.wmsName) and
                    (GoodMovement.departmentId eq request.departmentId) and (Sku.skuGroupId eq 1) and (GmItem.status eq "A")}

        if (request.fromProductionDate != null) {
            query.andWhere {
                GoodMovement.productionDate.date() greaterEq stringLiteral(request.fromProductionDate)
            }
        }
        if (request.toProductionDate != null) {
            query.andWhere {
                GoodMovement.productionDate.date() lessEq stringLiteral(request.toProductionDate)
            }
        }
        if (!request.code.isNullOrEmpty()) {
            query.andWhere {
                GoodMovement.code eq request.code
            }
        }

        return query.toList()
    }
}