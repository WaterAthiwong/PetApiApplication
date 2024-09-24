package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object StoreLocation: IntIdTable("store_location") {
    val storeZoneId: Column<Int> = integer("store_zone_id")
    val warehouseId: Column<Int> = integer("warehouse_id")
    val code : Column<String> = varchar("code", 45)
    val name : Column<String> = varchar("name", 45)
    val status : Column<String> = varchar("status", 1)
}