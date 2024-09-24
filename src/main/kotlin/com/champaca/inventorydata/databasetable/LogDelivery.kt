package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object LogDelivery: IntIdTable("cpc_log_delivery") {
    val supplierId: Column<Int> = integer("supplier_id")
    // Po No and delivery round are the two attributes to identify the delivery.
    val poNo: Column<String> = varchar("po_no", 32)
    val deliveryRound: Column<Int> = integer("round")
    val forestryBook: Column<String> = varchar("forestry_book", 16)
    val forestryBookNo: Column<String> = varchar("forestry_book_no", 4)
    val lotNo: Column<String?> = varchar("lot_no", 32).nullable()
    val fsc: Column<Boolean> = bool("fsc")
    // If FSC is true, the user needs to fill invoice no and fsc no too.
    val invoiceNo: Column<String?> = varchar("invoice_no", 100).nullable()
    val fscNo: Column<String?> = varchar("fsc_no", 32).nullable()
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val status: Column<String> = varchar("status", 1)
}