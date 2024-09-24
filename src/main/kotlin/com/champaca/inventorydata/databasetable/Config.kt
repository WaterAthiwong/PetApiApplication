package com.champaca.inventorydata.databasetable

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.time.LocalDateTime

object Config: IntIdTable("cpc_config") {
    val format = Json { prettyPrint = true }

    val name: Column<String> = varchar("name", 255)
    val valueInt: Column<Int?> = integer("value_int").nullable()
    val valueString: Column<String?> = varchar("value_str", 1024).nullable()
    val createdAt: Column<LocalDateTime> = datetime("created_at")
}