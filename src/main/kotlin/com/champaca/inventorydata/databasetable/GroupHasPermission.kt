package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object GroupHasPermission: IntIdTable("group_has_permission") {
    val groupId: Column<Int> = integer("group_id")
    val permissionId: Column<Int> = integer("permission_id")
}