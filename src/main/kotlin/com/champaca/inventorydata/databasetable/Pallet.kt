package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Pallet: IntIdTable("cpc_pallet") {
    val storeLocationId = reference("store_location_id", StoreLocation).nullable()
    val code: Column<String> = varchar("code", 45)
    val updatedAt: Column<LocalDateTime> = datetime("updated_at")
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val status: Column<String> = varchar("status", 1)
}