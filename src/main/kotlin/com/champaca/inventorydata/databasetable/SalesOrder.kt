package com.champaca.inventorydata.databasetable

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.time.LocalDateTime

object SalesOrder: IntIdTable("cpc_sales_order") {
    val odooId: Column<Int> = integer("odoo_id")
    val name: Column<String> = varchar("name", 255)
    val partnerId: Column<Int> = integer("partner_id")
    val supplierId: Column<Int> = integer("supplier_id")
    val warehouseId: Column<Int> = integer("warehouse_id")
    val state: Column<String> = varchar("state", 24)
    val deliveryState: Column<String> = varchar("delivery_state", 24)
    val orderedAt: Column<LocalDateTime> = datetime("ordered_at")
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val updatedAt: Column<LocalDateTime> = datetime("updated_at")
}