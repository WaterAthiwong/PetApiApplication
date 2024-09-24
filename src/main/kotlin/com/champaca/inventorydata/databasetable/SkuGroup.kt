package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object SkuGroup: IntIdTable("sku_group") {
    val name : Column<String> = varchar("name", 100)
    val erpMainGroupId: Column<Int> = integer("erp_main_group_id")
    val erpMainGroupName : Column<String> = varchar("erp_main_group_name", 45)
    val erpGroupCode : Column<String> = varchar("erp_group_code", 45)
    val erpGroupName : Column<String> = varchar("erp_group_name", 64)
    val status : Column<String> = varchar("status", 1)
}