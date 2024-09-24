package com.champaca.inventorydata.odoo.usecase.out

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.SupplierDao.Companion.SUPCUST
import com.champaca.inventorydata.databasetable.dao.SupplierDao.Companion.SUPPLIER
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.odoo.OdooService
import com.champaca.inventorydata.odoo.model.out.CreatePurchaseOrderRequest
import com.champaca.inventorydata.odoo.request.out.SawnTimberPurchaseOrderRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class SawnTimberPurchaseOrderUseCase(
    val dataSource: DataSource,
    val odooService: OdooService
) {
    companion object {
        val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    @Value("\${odoo.purchase.orderType.foreignWood.id}")
    lateinit var foreignWoodId: String

    @Value("\${odoo.purchase.orderType.localWood.id}")
    lateinit var localWoodId: String

    fun getData(request: SawnTimberPurchaseOrderRequest) {
        Database.connect(dataSource)

        transaction {
            addLogger(ExposedInfoLogger)
        }
    }

    fun sendToOdoo(request: SawnTimberPurchaseOrderRequest): List<CreatePurchaseOrderRequest> {
        Database.connect(dataSource)

        var results = listOf<CreatePurchaseOrderRequest>()
        transaction {
            addLogger(ExposedInfoLogger)

            val stocks = getStock(request)
            results = createOdooPurchaseOrderRequest(stocks)
        }

        results.forEach {
            odooService.createPurchaseOrder(it)
        }
        return results
    }

    private fun getStock(request: SawnTimberPurchaseOrderRequest): List<ResultRow> {
        val joins = GoodMovement.join(GmItem, JoinType.INNER) { (GoodMovement.id eq GmItem.goodMovementId) and (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) }
            .join(Sku, JoinType.INNER) { GmItem.skuId eq Sku.id }
            .join(LotNo, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(Supplier, JoinType.INNER) { Supplier.id eq GoodMovement.supplierId }
            .join(PileHasLotNo, JoinType.INNER) { PileHasLotNo.lotNoId eq LotNo.id }
            .join(Pile, JoinType.INNER) { Pile.id eq PileHasLotNo.pileId }
        val query = joins.select(
            GoodMovement.id,
            Supplier.erpCode,
            Supplier.name,
            GoodMovement.poNo,
            GoodMovement.lotNo,
            GoodMovement.extraAttributes,
            GmItem.qty,
            Sku.erpCode,
            Sku.name,
            Sku.matCode,
            Pile.code
        )
            .where { (GoodMovement.productionDate eq LocalDate.parse(request.receivedDate, DATE_FORMAT)) and
                    (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) and
                    (GoodMovement.departmentId eq request.departmentId) and (GoodMovement.status eq "A") and
                    (Supplier.type inList listOf(SUPPLIER, SUPCUST)) and
                    (LotNo.status eq "A") and (GmItem.status eq "A") }
        return query.toList()
    }

    private fun createOdooPurchaseOrderRequest(rows: List<ResultRow>): List<CreatePurchaseOrderRequest> {
        val rowsByPoNo = rows.groupBy { it[GoodMovement.poNo]!! + it[GoodMovement.lotNo]!! }
        val results = rowsByPoNo.map { (poNo, rows) ->
            val firstItem = rows.first()
            val extraAttributes = firstItem[GoodMovement.extraAttributes]
            val orderType = if (extraAttributes == null) {
                localWoodId.toInt()
            } else {
                val country = extraAttributes["countryOfOrigin"]!!.lowercase()
                if (country == "thailand") {
                    localWoodId.toInt()
                } else {
                    foreignWoodId.toInt()
                }
            }
            val rowByItem = rows.groupBy { it[Sku.erpCode]!! }
            val orderLines = rowByItem.map { (productId, itemRows) ->
                val firstRow = itemRows.first()
                CreatePurchaseOrderRequest.OrderLine(
                    productId = productId.toInt(),
                    name = firstRow[Sku.name],
                    quantity = itemRows.sumOf { it[GmItem.qty] },
//                    unitPrice = firstRow[RawMaterialCost.unitCostM3],
                    unitPrice = BigDecimal.ONE,
                    lotLines = listOf()
                )
            }
            CreatePurchaseOrderRequest(
                partnerId = 1,
                vendorReference = firstItem[GoodMovement.poNo] ?: "",
                orderType = orderType,
                orderLines = orderLines,
                prNo = firstItem[GoodMovement.poNo] ?: "",
            )
        }
        return results
    }
}