package com.champaca.inventorydata.databasetable

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.time.LocalDateTime

object LotGroup: IntIdTable("lot_group") {
    val format = Json { prettyPrint = true }

    val code : Column<String> = varchar("code", 45)
    val lotGroupId: Column<Int?> = integer("lot_group_id").nullable()
    val refCode: Column<String?> = varchar("ref_code", 45).nullable()
    val extraAttributes: Column<Map<String, String>?> = json<Map<String, String>>("extra_attributes", format).nullable()
    val createdAt : Column<LocalDateTime> = datetime("created_at")
    val updatedAt : Column<LocalDateTime> = datetime("updated_at")
    val status : Column<String> = varchar("status", 1)
}