package com.champaca.inventorydata.odoo.usecase.out

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.common.ChampacaConstant.FORESTRY_BOOK
import com.champaca.inventorydata.common.ChampacaConstant.FORESTRY_BOOK_NO
import com.champaca.inventorydata.common.ChampacaConstant.LOG_NO
import com.champaca.inventorydata.common.ChampacaConstant.VOLUMN_M3
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.odoo.OdooService
import com.champaca.inventorydata.odoo.model.out.CreatePurchaseOrderRequest
import com.champaca.inventorydata.odoo.request.out.LogPurchaseOrderRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class LogPurchaseOrderUseCase(
    val dataSource: DataSource,
    val odooService: OdooService
) {
    companion object {
        val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    @Value("\${odoo.purchase.orderType.localWood.id}")
    lateinit var localWoodId: String

    val logs = listOf(13749, 13747) // TODO - remove after test

    fun getData(request: LogPurchaseOrderRequest) {
        Database.connect(dataSource)

        transaction {
            addLogger(ExposedInfoLogger)
        }
    }

    fun sendToOdoo(request: LogPurchaseOrderRequest): List<CreatePurchaseOrderRequest> {
        Database.connect(dataSource)

        var results = listOf<CreatePurchaseOrderRequest>()
        transaction {
            addLogger(ExposedInfoLogger)

            val logs = getLogs(request)
            results = createOdooPurchaseOrderRequest(logs)
        }
//        results.forEach {
//            odooService.createPurchaseOrder(it)
//        }

        return results
    }

    private fun getLogs(request: LogPurchaseOrderRequest): List<ResultRow> {
        val joins = GoodMovement.join(GmItem, JoinType.INNER) { (GoodMovement.id eq GmItem.goodMovementId) and (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) }
            .join(Sku, JoinType.INNER) { GmItem.skuId eq Sku.id }
            .join(LotNo, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(Supplier, JoinType.INNER) { Supplier.id eq GoodMovement.supplierId }
            .join(Log, JoinType.INNER) { Log.refCode eq LotNo.refCode }
            .join(RawMaterialCost, JoinType.INNER) { (RawMaterialCost.poNo eq Log.batchNo) and (RawMaterialCost.type eq "log") }
        val query = joins.select(
            GoodMovement.id,
            Supplier.erpCode,
            Supplier.name,
            GoodMovement.poNo,
            Sku.id, // TODO - remove this after test
            Sku.erpCode,
            Sku.name,
            LotNo.refCode,
            LotNo.extraAttributes,
            Log.batchNo,
            RawMaterialCost.unitCostM3,
            GoodMovement.extraAttributes
        )
            .where { (Log.exportedToWmsAt.date() eq LocalDate.parse(request.receivedDate, DATE_FORMAT)) and (LotNo.status eq "A") and
                    (GmItem.status eq "A") and (Log.status eq "A") }
        return query.toList()
    }

    // TODO - remove this after test
    private fun getRandomCode(skuId: Int): Int {
        return when (skuId % 2) {
            0 -> logs[0]
            else -> logs[1]
        }
    }

    private fun createOdooPurchaseOrderRequest(rows: List<ResultRow>): List<CreatePurchaseOrderRequest> {
        val rowByPoNo = rows.groupBy { it[GoodMovement.poNo] }
        val results = rowByPoNo.map { (poNo, rows) ->
            val firstItem = rows.first()
            val rowsByItem = rows.groupBy { getRandomCode(it[Sku.id].value) to it[Log.batchNo] }
            val orderLines = rowsByItem.map { (pair, rows) ->
                val productId = pair.first
                val firstRow = rows.first()
                val lotLines = rows.map {
                    val extra = it[LotNo.extraAttributes]!!
                    val logNo = extra[LOG_NO]!!
                    val volumnM3 = extra[VOLUMN_M3]!!.toBigDecimal()
                    val forestryBook = extra[FORESTRY_BOOK]!!
                    val forestryBookNo = extra[FORESTRY_BOOK_NO]!!
                    CreatePurchaseOrderRequest.LotLine(
                        lotName = it[LotNo.refCode],
                        quantity = volumnM3,
                        noTrencher = logNo,
                        bookNumber = forestryBook,
                        serialNumber = forestryBookNo,
                        forest = firstRow[Supplier.name]
                    )
                }
                CreatePurchaseOrderRequest.OrderLine(
                    productId = productId.toInt(),
                    name = firstRow[Sku.name],
                    quantity = lotLines.sumOf { it.quantity },
                    unitPrice = firstRow[RawMaterialCost.unitCostM3],
                    lotLines = lotLines
                )
            }
            val extraAttributes = firstItem[GoodMovement.extraAttributes]
            val contractNo = extraAttributes?.get("contractNo") ?: ""
            CreatePurchaseOrderRequest(
                partnerId = firstItem[Supplier.erpCode]!!.toInt(),
                vendorReference = firstItem[GoodMovement.poNo] ?: "",
                orderType = localWoodId.toInt(),
                orderLines = orderLines,
                prNo = firstItem[GoodMovement.poNo] ?: "",
                contractNo = contractNo
            )
        }
        return results
    }
}