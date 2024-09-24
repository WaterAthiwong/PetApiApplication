package com.champaca.inventorydata.odoo.usecase.out

import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.SalesOrderDao
import com.champaca.inventorydata.databasetable.dao.SupplierDao.Companion.CUSTOMER
import com.champaca.inventorydata.databasetable.dao.SupplierDao.Companion.SUPCUST
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.odoo.model.out.SalesOrderSendDeliveryRequest
import com.champaca.inventorydata.odoo.request.out.DeliveryOrderRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class DeliveryOrderUseCase(
    val dataSource: DataSource
) {
    companion object {
        const val WAREHOUSE_FG_DEPARTMENT_ID = 10
        val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    @Value("\${odoo.stock.location.warehouseFg.id}")
    lateinit var warehouseFgLocationId: String

    val logger = LoggerFactory.getLogger(DeliveryOrderUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun getData(request: DeliveryOrderRequest) {
        Database.connect(dataSource)

        var results = listOf<SalesOrderSendDeliveryRequest>()
        transaction {
            addLogger(exposedLogger)

        }
    }

    fun sendToOdoo(request: DeliveryOrderRequest): Map<Int, SalesOrderSendDeliveryRequest> {
        Database.connect(dataSource)

        var results = mapOf<Int, SalesOrderSendDeliveryRequest>()
        transaction {
            addLogger(exposedLogger)
            val deliverables = getDeliverables(request)
            val salesOrders = getSalesOrders(deliverables.map { it[FinishedGoodStickerBatch.odooSalesOrderNo] }.distinct())
            results = createSalesOrderSenDeliveryRequest(deliverables, salesOrders)
        }
        return results
    }

    private fun getDeliverables(request: DeliveryOrderRequest): List<ResultRow> {
        val joins = GoodMovement.join(GmItem, JoinType.INNER) { (GoodMovement.id eq GmItem.goodMovementId) }
            .join(Sku, JoinType.INNER) { GmItem.skuId eq Sku.id }
            .join(LotNo, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(PileHasLotNo, JoinType.INNER) { LotNo.id eq PileHasLotNo.lotNoId }
            .join(Pile, JoinType.INNER) { (PileHasLotNo.pileId eq Pile.id) }
            .join(Supplier, JoinType.INNER) { Supplier.id eq GoodMovement.supplierId }
            .join(FinishedGoodSticker, JoinType.INNER) { FinishedGoodSticker.pileId eq Pile.id }
            .join(FinishedGoodStickerBatch, JoinType.INNER) { FinishedGoodStickerBatch.id eq FinishedGoodSticker.batchId }
        val query = joins.select(
            GoodMovement.id,
            GoodMovement.type,
            GoodMovement.orderNo,
            GmItem.qty,
            Sku.id,
            Sku.erpCode,
            Sku.matCode,
            Pile.code,
            FinishedGoodStickerBatch.odooSalesOrderNo,
            FinishedGoodStickerBatch.salesOrderLineNo
        )
            .where {
                (GmItem.createdAt.date() eq LocalDate.parse(request.deliveredDate, StockMovementUseCase.DATE_FORMAT)) and
                (GoodMovement.departmentId eq WAREHOUSE_FG_DEPARTMENT_ID) and (Supplier.type inList listOf(CUSTOMER, SUPCUST)) and
                (GoodMovement.type eq GoodMovementType.PICKING_ORDER.wmsName) and (LotNo.status eq "A") and (GmItem.status eq "A")
            }

        if (request.salesOrderNos.isNotEmpty()) {
            query.andWhere { GoodMovement.orderNo inList request.salesOrderNos }
        }
        return query.toList()
    }

    private fun getSalesOrders(salesOrderNoNames: List<String>): Map<String, SalesOrderDao> {
        return SalesOrderDao.find { SalesOrder.name inList salesOrderNoNames }.associateBy { it.name }
    }

    private fun createSalesOrderSenDeliveryRequest(deliverables: List<ResultRow>, salesOrders: Map<String, SalesOrderDao>): Map<Int, SalesOrderSendDeliveryRequest> {
        val results = deliverables.groupBy { it[FinishedGoodStickerBatch.odooSalesOrderNo] }
            .mapValues { (soNo, rows) ->
                val soDao = salesOrders[soNo]!!
                val orderLines = rows.map { row ->
                    val solineDao = soDao.lines.toList().get(row[FinishedGoodStickerBatch.salesOrderLineNo].toInt() - 1)
                    SalesOrderSendDeliveryRequest.OrderLine(
                        salesLineId = solineDao.odooId,
                        productId =  row[Sku.erpCode]!!.toInt(),
                        packNo = row[Pile.code],
                        doneQty = row[GmItem.qty]
                    )
                }
                SalesOrderSendDeliveryRequest(
                    dateDone = LocalDateTime.now().format(DATETIME_FORMAT),
                    locationId = warehouseFgLocationId.toInt(),
                    orderLines = orderLines
                )
            }
            .mapKeys { salesOrders[it.key]!!.odooId }
        return results
    }
}