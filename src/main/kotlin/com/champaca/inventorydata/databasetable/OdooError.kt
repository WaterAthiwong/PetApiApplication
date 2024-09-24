package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object OdooError: IntIdTable("cpc_odoo_error") {
    val action: Column<String> = varchar("action", 45)
    val payload: Column<String> = varchar("payload", 1024)
    val error: Column<String> = varchar("error", 255)
    val createdAt: Column<LocalDateTime> = datetime("created_at")
}