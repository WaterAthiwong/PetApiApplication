package com.champaca.inventorydata.data.report.model.impl

import com.champaca.inventorydata.common.ChampacaConstant
import com.champaca.inventorydata.data.report.model.YieldCalculator
import com.champaca.inventorydata.data.report.model.YieldResult
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class SawmillYieldCalculator: YieldCalculator {

    companion object {
        const val LOG_GROUP = 1
        const val WASTE_SKU_GROUP_ID = 114
    }
    override fun calculateYield(departmentId: Int, startDate: String, endDate: String): List<YieldResult> {
        val logEntries = getLogPickingOrder(departmentId, startDate, endDate)
            .groupBy { it[GoodMovement.jobNo]!! }
            .mapValues { (_, values) ->
                val first = values.first()
                val volumnM3 = values.sumOf { it[LotNo.extraAttributes]!!["volumnM3"]!!.toBigDecimal() }
                YieldResult(
                    jobNo = first[GoodMovement.jobNo]!!,
                    lotNo = first[GoodMovement.lotNo],
                    manufacturingLine = first[ManufacturingLine.name],
                    supplier = first[Supplier.name],
                    incomings = values.size,
                    incomingVolumnM3 = volumnM3.setScale(2, RoundingMode.HALF_UP),
                    incomingVolumnFt3 = volumnM3.multiply(ChampacaConstant.M3_TO_FT3).setScale(2, RoundingMode.HALF_UP),
                    outgoings = 0,
                    outgoingVolumnFt3 = BigDecimal.ZERO,
                    yield = BigDecimal.ZERO,
                    status = null,
                    type = "log"
                )
            }

        val woodEntries = getWoodsPickingOrder(departmentId, startDate, endDate)
            .groupBy { it[GoodMovement.jobNo]!! }
            .mapValues { (_, values) ->
                val first = values.first()
                val volumnFt3 = values.sumOf { it[Sku.volumnFt3].multiply(it[GmItem.qty]) }
                YieldResult(
                    jobNo = first[GoodMovement.jobNo]!!,
                    lotNo = first[GoodMovement.lotNo],
                    manufacturingLine = first[ManufacturingLine.name],
                    supplier = first[Supplier.name],
                    incomings = values.sumOf { it[GmItem.qty] }.toInt(),
                    incomingVolumnM3 = volumnFt3.divide(ChampacaConstant.M3_TO_FT3, 2, RoundingMode.HALF_UP),
                    incomingVolumnFt3 = volumnFt3.setScale(2, RoundingMode.HALF_UP),
                    outgoings = 0,
                    outgoingVolumnFt3 = BigDecimal.ZERO,
                    yield = BigDecimal.ZERO,
                    status = null,
                    type = "rework"
                )
            }

        val allEntries = logEntries + woodEntries
        val jobNos = allEntries.keys.toList()
        val outGoingWoods = getWoodReceive(departmentId, startDate, endDate, jobNos).groupBy { it[GoodMovement.jobNo] }

        allEntries.forEach { (jobNo, entry) ->
            val woods = outGoingWoods[jobNo]
            if (woods != null) {
                entry.outgoings = woods.sumOf { it[GmItem.qty] }.toInt()
                entry.outgoingVolumnFt3 = woods.sumOf { it[Sku.volumnFt3].multiply(it[GmItem.qty]) }.setScale(2, RoundingMode.HALF_UP)
                entry.yield = entry.outgoingVolumnFt3.multiply(100.toBigDecimal()).divide(entry.incomingVolumnFt3, 2, RoundingMode.HALF_UP)
            }
        }

        return allEntries.values.toList()
    }

    private fun getLogPickingOrder(departmentId: Int, startDate: String, endDate: String): List<ResultRow> {
        val joins = GoodMovement.join(GmItem, JoinType.INNER) { GoodMovement.id eq GmItem.goodMovementId }
            .join(LotNo, JoinType.INNER) { LotNo.id eq GmItem.lotNoId }
            .join(Sku, JoinType.INNER) { Sku.id eq GmItem.skuId }
            .join(Supplier, JoinType.INNER) { Supplier.id eq GoodMovement.supplierId }
            .join(ManufacturingLine, JoinType.INNER) { ManufacturingLine.id eq GoodMovement.manufacturingLineId }
        val query = joins.select(
            GoodMovement.jobNo,
            GoodMovement.lotNo,
            Supplier.name,
            LotNo.extraAttributes,
            ManufacturingLine.name,
        )
            .where{ (LotNo.status eq "A") and (GmItem.status eq "A") and (GoodMovement.status eq "A") and
                    (GoodMovement.departmentId eq departmentId) and (GoodMovement.type eq GoodMovementType.PICKING_ORDER.wmsName) and
                    (Sku.skuGroupId eq LOG_GROUP) and
                    (GoodMovement.productionDate greaterEq LocalDate.parse(startDate, dateFormat)) and
                    (GoodMovement.productionDate lessEq LocalDate.parse(endDate, dateFormat))
            }

        return query.toList()
    }

    private fun getWoodsPickingOrder(departmentId: Int, startDate: String, endDate: String): List<ResultRow> {
        val joins = GoodMovement.join(GmItem, JoinType.INNER) { GoodMovement.id eq GmItem.goodMovementId }
            .join(LotNo, JoinType.INNER) { LotNo.id eq GmItem.lotNoId }
            .join(Sku, JoinType.INNER) { Sku.id eq GmItem.skuId }
            .join(Supplier, JoinType.LEFT) { Supplier.id eq GoodMovement.supplierId }
            .join(ManufacturingLine, JoinType.INNER) { ManufacturingLine.id eq GoodMovement.manufacturingLineId }
        val query = joins.select(
            GoodMovement.jobNo,
            GoodMovement.lotNo,
            Supplier.name,
            Sku.volumnFt3,
            GmItem.qty,
            ManufacturingLine.name,
        )
            .where{ (LotNo.status eq "A") and (GmItem.status eq "A") and (GoodMovement.status eq "A") and
                    (GoodMovement.departmentId eq departmentId) and (GoodMovement.type eq GoodMovementType.PICKING_ORDER.wmsName) and
                    (Sku.skuGroupId neq LOG_GROUP) and
                    (GoodMovement.productionDate greaterEq LocalDate.parse(startDate, dateFormat)) and
                    (GoodMovement.productionDate lessEq LocalDate.parse(endDate, dateFormat)) }
        return query.toList()
    }

    private fun getWoodReceive(departmentId: Int, startDate: String, endDate: String, jobNos: List<String>): List<ResultRow> {
        val joins = GoodMovement.join(GmItem, JoinType.INNER) { GoodMovement.id eq GmItem.goodMovementId }
            .join(LotNo, JoinType.INNER) { LotNo.id eq GmItem.lotNoId }
            .join(Sku, JoinType.INNER) { Sku.id eq GmItem.skuId }
            .join(ManufacturingLine, JoinType.INNER) { ManufacturingLine.id eq GoodMovement.manufacturingLineId }
        val query = joins.select(
            GoodMovement.jobNo,
            Sku.volumnFt3,
            GmItem.qty,
        )
            .where{ (LotNo.status eq "A") and (GmItem.status eq "A") and (GoodMovement.status eq "A") and
                    (GoodMovement.departmentId eq departmentId) and (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) and
                    (GoodMovement.jobNo inList jobNos) and
                    (GoodMovement.productionDate greaterEq LocalDate.parse(startDate, dateFormat)) and
                    (GoodMovement.productionDate lessEq LocalDate.parse(endDate, dateFormat)) and
                    (Sku.skuGroupId neq WASTE_SKU_GROUP_ID)
            }
        return query.toList()
    }
}