package com.champaca.inventorydata.odoo.usecase

import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.SalesOrder
import com.champaca.inventorydata.databasetable.SalesOrderLine
import com.champaca.inventorydata.databasetable.Supplier
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
class CreateSalesOrderUseCase(
    val dataSource: DataSource,
    val odooService: OdooService
) {
    val DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
    val logger = LoggerFactory.getLogger(CreateSalesOrderUseCase::class.java)
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
                odooService.recordError("CreateSalesOrder", payload, "matching supplier not found")
                return@transaction
            }

            val now = LocalDateTime.now()
            val salesOrderId = SalesOrder.insertAndGetId {
                it[odooId] = request.odooId
                it[name] = request.name
                it[partnerId] = request.partnerId
                it[supplierId] = supplier.id.value
                it[warehouseId] = request.warehouseId
                it[state] = request.state
                it[deliveryState] = request.deliveryState
                it[orderedAt] = LocalDateTime.parse(request.dateOrder, DATETIME_FORMAT)
                it[createdAt] = now
                it[updatedAt] = now
            }

            SalesOrderLine.batchInsert(request.orderLines) { line ->
                this[SalesOrderLine.salesOrderId] = salesOrderId
                this[SalesOrderLine.odooId] = line.id
                this[SalesOrderLine.name] = line.name
                this[SalesOrderLine.productId] = line.productId
                this[SalesOrderLine.qtyToDeliver] = line.qtyToDeliver.toBigDecimal()
                this[SalesOrderLine.orderedQty] = line.orderedQty.toBigDecimal()
                this[SalesOrderLine.createdAt] = now
                this[SalesOrderLine.updatedAt] = now
            }
        }
    }
}