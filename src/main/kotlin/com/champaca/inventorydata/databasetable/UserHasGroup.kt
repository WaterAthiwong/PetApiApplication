package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.math.BigDecimal
import java.time.LocalDateTime

object UserHasGroup: IntIdTable("user_has_group") {
    val userId: Column<Int> = integer("user_id")
    val groupId: Column<Int> = integer("group_id")
}