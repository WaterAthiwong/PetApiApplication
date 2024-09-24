package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object Group: IntIdTable("group") {
    val name: Column<String> = varchar("name", 45)
    val status: Column<String> = varchar("status", 1)
}