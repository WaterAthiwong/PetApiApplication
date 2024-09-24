package com.champaca.inventorydata.databasetable

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.time.LocalDateTime

object LogRelocation: IntIdTable("cpc_log_relocation") {
    val lotNoId: Column<Int> = integer("lot_no_id")
    val fromStoreLocationId: Column<Int> = integer("from_store_location_id")
    val toStoreLocationId: Column<Int> = integer("to_store_location_id")
    val userId: Column<Int> = integer("user_id")
    val createdAt: Column<LocalDateTime> = datetime("created_at")
}