package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object Supplier: IntIdTable("supplier") {
    val type: Column<String> = varchar("type", 24)
    val name : Column<String> = varchar("name", 100)
    val taxNo : Column<String?> = varchar("tax_no", 45).nullable()
    val address : Column<String?> = text("address").nullable()
    val phone : Column<String?> = varchar("phone", 45).nullable()
    val email : Column<String?> = varchar("email", 45).nullable()
    val contact : Column<String?> = varchar("contact", 100).nullable()
    val erpCode : Column<String?> = varchar("erp_code", 45).nullable()
    val remark : Column<String?> = text("remark").nullable()
    val status : Column<String> = varchar("status", 1)
}