package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ReceivedLogIncident: IntIdTable("cpc_received_log_incident") {
    val userId: Column<Int> = integer("user_id")
    val logId: Column<Int?> = integer("cpc_log_id").nullable()
    val barcode: Column<String> = varchar("barcode", 12)
    val errorCode: Column<Int> = integer("error_code")
    val isSolved: Column<Boolean> = bool("is_solved")
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val updatedAt: Column<LocalDateTime> = datetime("updated_at")
}