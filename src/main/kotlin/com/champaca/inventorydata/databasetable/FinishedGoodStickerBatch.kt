package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate
import java.time.LocalDateTime

object FinishedGoodStickerBatch: IntIdTable("cpc_fg_sticker_batch") {
    val suppilerId: Column<Int> = integer("supplier_id")
    val code: Column<String> = varchar("code", 12)
    val salesOrderNo: Column<String> = varchar("sales_order_no", 64)
    val odooSalesOrderNo: Column<String> = varchar("odoo_sales_order_no", 64)
    val salesOrderLineNo: Column<String> = varchar("sales_order_line_no", 3)
    val format: Column<String> = varchar("format", 7)
    val productionDate: Column<LocalDate> = date("production_date")
    val remark: Column<String?> = varchar("remark", 64).nullable()
    val remark2: Column<String?> = varchar("remark2", 64).nullable()
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val status: Column<String> = varchar("status", 1)
}