package com.champaca.inventorydata.databasetable

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.time.LocalDate
import java.time.LocalDateTime

object GoodMovement: IntIdTable("good_movement") {
    val format = Json { prettyPrint = true }

    val departmentId = reference("department_id", Department)
    val goodReceiptGoodMovementId = reference("good_movement_id", GoodMovement).nullable()
    val transferGoodMovementId = reference("good_movement_transfer_id", GoodMovement).nullable()
    val manufacturingLineId: Column<Int?> = integer("manufacturing_line_id").nullable()
    val supplierId: Column<Int?> = integer("supplier_id").nullable()
    val userId: Column<Int> = integer("user_id")
    val code : Column<String> = varchar("code", 45)
    val type : Column<String> = varchar("type", 45)
    val createdAt : Column<LocalDateTime> = datetime("created_at")
    val orderNo : Column<String?> = varchar("order_no", 45).nullable()
    val jobNo : Column<String?> = varchar("job_no", 45).nullable()
    val poNo : Column<String?> = varchar("po_no", 45).nullable()
    val invoiceNo : Column<String?> = varchar("invoice_no", 45).nullable()
    val lotNo : Column<String?> = varchar("lot_no", 45).nullable()
    val productionDate : Column<LocalDate> = date("production_date")
    val remark : Column<String?> = text("remark").nullable()
    val approveUserId: Column<Int?> = integer("approve_user_id").nullable()
    val closeUserId: Column<Int?> = integer("close_user_id").nullable()
    val approvedAt : Column<LocalDateTime?> = datetime("approved_at").nullable()
    val closedAt : Column<LocalDateTime?> = datetime("closed_at").nullable()
    val isTransfer: Column<Boolean> = bool("is_transfer")
    val extraAttributes: Column<Map<String, String>?> = json<Map<String, String>>("extra_attributes", format).nullable()
    val productType: Column<String?> = varchar("doc_product_type", 45).nullable()
    val status : Column<String> = varchar("status", 1)
}