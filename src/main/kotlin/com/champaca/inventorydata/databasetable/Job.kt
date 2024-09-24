package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDate
import java.time.LocalDateTime

object Job: IntIdTable("cpc_job") {
    val jobNo: Column<String> = varchar("job_no", 45)
    val orderNo: Column<String?> = varchar("order_no", 45).nullable()
    val invoiceNo: Column<String?> = varchar("invoice_no", 45).nullable()
    val lotNo: Column<String?> = varchar("lot_no", 45).nullable()
    val fsc: Column<Boolean?> = bool("fsc").nullable()
    val productionDate: Column<LocalDate> = date("production_date")
    val endDate: Column<LocalDate?> = date("end_date").nullable()
    val createdAt: Column<LocalDateTime> = datetime("created_at")
}