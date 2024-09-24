package com.champaca.inventorydata.databasetable

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.math.BigDecimal
import java.time.LocalDateTime

object SalesOrderLine: IntIdTable("cpc_sales_order_line") {
    val salesOrderId = reference("cpc_sales_order_id", SalesOrder)
    val odooId: Column<Int> = integer("odoo_id")
    val name: Column<String> = varchar("name", 255)
    val productId: Column<Int> = integer("product_id")
    val qtyToDeliver: Column<BigDecimal> = decimal("qty_to_deliver", 10, 5)
    val orderedQty: Column<BigDecimal> = decimal("ordered_qty", 10, 5)
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val updatedAt: Column<LocalDateTime> = datetime("updated_at")
}