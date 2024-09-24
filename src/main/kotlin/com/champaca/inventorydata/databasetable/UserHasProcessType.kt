package com.champaca.inventorydata.databasetable

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.time.LocalDateTime

object UserHasProcessType: Table("user_has_process_type") {
    val userId: Column<Int> = integer("user_id")
    val processTypeId: Column<Int> = integer("process_type_id")
}