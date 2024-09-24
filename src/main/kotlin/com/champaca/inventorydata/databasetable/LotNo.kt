package com.champaca.inventorydata.databasetable

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.time.LocalDateTime

object LotNo: IntIdTable("lot_no") {
    val format = Json { prettyPrint = true }

    val lotGroupId = reference("lot_group_id", LotGroup)
    val code: Column<String> = varchar("code", 45)
    val refCode: Column<String> = varchar("ref_code", 45)
    val additionalField: Column<String> = text("additional_field")
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val updatedAt: Column<LocalDateTime> = datetime("updated_at")
    val extraAttributes = json<Map<String, String>>("extra_attributes", format).nullable()
    val status: Column<String> = varchar("status", 1)
}