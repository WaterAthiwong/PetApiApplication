package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object SkuRequest: IntIdTable("cpc_sku_request") {
    val userId: Column<Int> = integer("user_id")
    val payload: Column<String> = varchar("payload", 512)
    val success: Column<Boolean> = bool("success")
    val error: Column<String> = varchar("error", 24)
    val createdAt: Column<LocalDateTime> = datetime("created_at")
}