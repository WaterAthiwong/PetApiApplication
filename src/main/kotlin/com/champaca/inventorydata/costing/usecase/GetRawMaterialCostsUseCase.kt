package com.champaca.inventorydata.costing.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.costing.model.RawMaterialCostData
import com.champaca.inventorydata.costing.request.GetRawMaterialCostsRequest
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.RawMaterialCostDao
import com.champaca.inventorydata.databasetable.dao.SupplierDao.Companion.SUPCUST
import com.champaca.inventorydata.databasetable.dao.SupplierDao.Companion.SUPPLIER
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class GetRawMaterialCostsUseCase(
    val dataSource: DataSource
) {
    companion object {
        const val BACK_DATES = 30.toLong()
        val DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val RECEIVING_DEPARTMENT_IDS = listOf(5, 6)
    }

    val logger = LoggerFactory.getLogger(GetRawMaterialCostsUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: GetRawMaterialCostsRequest): List<RawMaterialCostData> {
        Database.connect(dataSource)

        var rmCosts = listOf<RawMaterialCostData>()
        transaction {
            addLogger(exposedLogger)
            rmCosts = getRmCosts(request)
            when(request.type) {
                RawMaterialCostDao.LOG -> {
                    rmCosts = fillLogQuantityAndVolumn(rmCosts)
                }
                RawMaterialCostDao.SAWN_TIMBER -> {
                    val poNos = rmCosts.map { it.poNo }
                    rmCosts += getCostListForImportedTimber(poNos)
                }
            }
        }

        return rmCosts
    }

    private fun getRmCosts(request: GetRawMaterialCostsRequest): List<RawMaterialCostData> {
        val joins = RawMaterialCost.join(Sku, JoinType.LEFT) { RawMaterialCost.skuId eq Sku.id }
            .join(Supplier, JoinType.INNER) { RawMaterialCost.supplierId eq Supplier.id }
        val query = joins.select(RawMaterialCost.id, RawMaterialCost.type, RawMaterialCost.poNo, RawMaterialCost.deliveryCycle,
            RawMaterialCost.unitCostM3, RawMaterialCost.unitCostFt3, RawMaterialCost.createdAt, Sku.id, Sku.matCode, Supplier.id, Supplier.name,
            RawMaterialCost.createdAt, RawMaterialCost.updatedAt)
            .where{ (RawMaterialCost.status eq "A") and (RawMaterialCost.type eq request.type) }
            .orderBy(RawMaterialCost.createdAt to SortOrder.DESC)
        return query.map {
            RawMaterialCostData(
                id = it[RawMaterialCost.id].value,
                supplierId = it[Supplier.id].value,
                supplier = it[Supplier.name],
                skuId = if(it[Sku.id] != null) it[Sku.id].value else 0,
                matCode = if(it[Sku.matCode] != null) it[Sku.matCode] else "",
                type = it[RawMaterialCost.type],
                poNo = it[RawMaterialCost.poNo],
                deliveryCycle = it[RawMaterialCost.deliveryCycle],
                unitCostM3 = it[RawMaterialCost.unitCostM3],
                unitCostFt3 = it[RawMaterialCost.unitCostFt3],
                createdAt = DATETIME.format(it[RawMaterialCost.createdAt]),
                updatedAt = DATETIME.format(it[RawMaterialCost.updatedAt])
            )
        }
    }

    private fun fillLogQuantityAndVolumn(rmCosts: List<RawMaterialCostData>): List<RawMaterialCostData>  {
        val pileNos = rmCosts.map { it.poNo }
        val pileNoMap = rmCosts.associateBy { it.poNo }
        val query = Log.select(Log.batchNo, Log.id.count(), Log.volumnM3.sum())
            .where { (Log.status eq "A") and (Log.batchNo inList pileNos) }
            .groupBy(Log.batchNo)
        query.toList().forEach { row ->
            val pileNo = row[Log.batchNo]
            val count = row[Log.id.count()]
            pileNoMap[pileNo]?.qty = count.toBigDecimal()
            pileNoMap[pileNo]?.volumnM3 = row[Log.volumnM3.sum()] ?: BigDecimal.ZERO
        }
        return rmCosts
    }

//    private fun fillingSawnTimberQuantity(rmCosts: List<RawMaterialCostData>): List<RawMaterialCostData> {
//
//    }

    private fun getCostListForImportedTimber(poNos: List<String>): List<RawMaterialCostData> {
        val backDate = LocalDateTime.now().minusDays(BACK_DATES).withHour(0).withMinute(0).withSecond(0)
        val joins = GoodMovement.join(GmItem, JoinType.INNER) { GoodMovement.id eq GmItem.goodMovementId }
            .join(Supplier, JoinType.INNER) { GoodMovement.supplierId eq Supplier.id }
            .join(Sku, JoinType.INNER) { GmItem.skuId eq Sku.id }
        val query = joins.select(GoodMovement.poNo, GoodMovement.lotNo, Supplier.id, Supplier.name, Sku.id, Sku.matCode,
                GmItem.qty, GoodMovement.productionDate)
            .where{ (GoodMovement.status eq "A") and (GmItem.status eq "A") and
                    (GoodMovement.createdAt greaterEq backDate) and (GoodMovement.poNo.isNotNull()) and
                    (GoodMovement.poNo neq "") and (GoodMovement.departmentId inList RECEIVING_DEPARTMENT_IDS) and
                    (Supplier.type inList listOf(SUPPLIER, SUPCUST)) }
            .orderBy(GoodMovement.productionDate to SortOrder.DESC)
        if (poNos.isNotEmpty()) {
            query.andWhere { GoodMovement.poNo notInList poNos }
        }

        return query.toList().groupBy { it[GoodMovement.poNo]!! to it[Sku.id].value }.mapValues { (pair, entries) ->
            val poNo = pair.first
            val skuId = pair.second
            val entry = entries.first()
            val lotNo = entry[GoodMovement.lotNo]
            RawMaterialCostData(
                supplierId = entry[Supplier.id].value,
                supplier = entry[Supplier.name],
                skuId = skuId,
                matCode = entry[Sku.matCode],
                type = RawMaterialCostDao.SAWN_TIMBER,
                poNo = poNo,
                deliveryCycle = 1
            ).apply {
                qty = entries.sumOf { it[GmItem.qty] }
                receivedDate = DATE.format(entry[GoodMovement.productionDate])
            }
        }.values.toList()
    }
}