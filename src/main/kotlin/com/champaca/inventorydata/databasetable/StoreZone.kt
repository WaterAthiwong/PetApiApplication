package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object StoreZone: IntIdTable("store_zone") {
    val buildingId: Column<Int> = integer("building_id")
    val departmentId: Column<Int> = integer("department_id")
    val code : Column<String> = varchar("code", 10)
    val name : Column<String> = varchar("name", 45)
    val erpStorageCode : Column<String> = varchar("erp_storage_code", 10)
    val erpStorageName : Column<String> = varchar("erp_storage_name", 45)
    val status : Column<String> = varchar("status", 1)
}