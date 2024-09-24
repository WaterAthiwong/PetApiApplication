package com.champaca.inventorydata.odoo.usecase

import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.SalesOrder
import com.champaca.inventorydata.databasetable.SalesOrderLine
import com.champaca.inventorydata.databasetable.Supplier
import com.champaca.inventorydata.databasetable.dao.SalesOrderDao
import com.champaca.inventorydata.databasetable.dao.SupplierDao
import com.champaca.inventorydata.odoo.OdooService
import com.champaca.inventorydata.odoo.request.UpsertSalesOrderRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class UpdateSalesOrderUseCase(
    val dataSource: DataSource,
    val odooService: OdooService
) {
    val DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
    val logger = LoggerFactory.getLogger(UpdateSalesOrderUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: UpsertSalesOrderRequest) {
        val payload = Json.encodeToString(request)
        logger.info("Payload: $payload")
        Database.connect(dataSource)
        transaction {
            addLogger(exposedLogger)
            val supplier = SupplierDao.find { (Supplier.erpCode eq request.partnerId.toString()) and
                    (Supplier.status eq "A") }
                .firstOrNull()
            if (supplier == null) {
                logger.error("Supplier with erp code ${request.partnerId} not found")
                odooService.recordError("UpdateSalesOrder", payload, "matching supplier not found")
                return@transaction
            }

            val salesOrder = SalesOrderDao.find { SalesOrder.odooId eq request.odooId }.firstOrNull()
            if (salesOrder == null) {
                logger.error("Sales order with odoo id ${request.odooId} not found")
                odooService.recordError("UpdateSalesOrder", payload, "matching sales order not found")
                return@transaction
            }

            val now = LocalDateTime.now()
            salesOrder.apply {
                odooId = request.odooId
                name = request.name
                partnerId = request.partnerId
                supplierId = supplier.id.value
                warehouseId = request.warehouseId
                state = request.state
                deliveryState = request.deliveryState
                orderedAt = LocalDateTime.parse(request.dateOrder, DATETIME_FORMAT)
                updatedAt = now
            }

            updateLines(salesOrder, request)
        }
    }

    private fun updateLines(salesOrder: SalesOrderDao, request: UpsertSalesOrderRequest) {
        val lines = salesOrder.lines
        val lineIds = request.orderLines.map { it.id }
        val linesToDelete = lines.filter { it.odooId !in lineIds }
        val linesToUpdate = lines.filter { it.odooId in lineIds }
        val linesToInsert = request.orderLines.filter { it.id !in linesToUpdate.map { it.odooId } }

        linesToDelete.forEach { it.delete() }
        linesToUpdate.forEach { line ->
            val requestLine = request.orderLines.first { it.id == line.odooId }
            line.apply {
                name = requestLine.name
                productId = requestLine.productId
                qtyToDeliver = requestLine.qtyToDeliver.toBigDecimal()
                orderedQty = requestLine.orderedQty.toBigDecimal()
                updatedAt = LocalDateTime.now()
            }
        }
        SalesOrderLine.batchInsert(linesToInsert) { line ->
            this[SalesOrderLine.salesOrderId] = salesOrder.id
            this[SalesOrderLine.odooId] = line.id
            this[SalesOrderLine.name] = line.name
            this[SalesOrderLine.productId] = line.productId
            this[SalesOrderLine.qtyToDeliver] = line.qtyToDeliver.toBigDecimal()
            this[SalesOrderLine.orderedQty] = line.orderedQty.toBigDecimal()
            this[SalesOrderLine.createdAt] = LocalDateTime.now()
            this[SalesOrderLine.updatedAt] = LocalDateTime.now()
        }
    }
}