package com.champaca.inventorydata.databasetable

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.math.BigDecimal
import java.time.LocalDateTime

object RawMaterialCost: IntIdTable("cpc_rm_cost") {
    val supplierId: Column<Int> = integer("supplier_id")
    val skuId: Column<Int?> = integer("sku_id").nullable()
    val type: Column<String> = varchar("type", 10)
    val poNo: Column<String> = varchar("po_no", 20)
    val deliveryCycle: Column<Int> = integer("delivery_cycle")
    val unitCostM3 : Column<BigDecimal> = decimal("unit_cost_m3", 10, 5)
    val unitCostFt3 : Column<BigDecimal> = decimal("unit_cost_ft3", 10, 5)
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val updatedAt: Column<LocalDateTime> = datetime("updated_at")
    val status: Column<String> = varchar("status", 1)
}