package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object Permission: IntIdTable("permission") {
    val name: Column<String> = varchar("name", 100)
}