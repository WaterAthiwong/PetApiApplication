package com.champaca.inventorydata.databasetable

import com.champaca.inventorydata.databasetable.PileTransaction.nullable
import com.champaca.inventorydata.databasetable.PileTransaction.uniqueIndex
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.time.LocalDateTime

object LogTransaction: IntIdTable("cpc_log_transaction") {
    val lotNoId: Column<Int?> = integer("lot_no_id").nullable().uniqueIndex()
    val fromGoodMovementId: Column<Int?> = integer("from_good_movement_id").nullable().uniqueIndex()
    val toGoodMovementId: Column<Int?> = integer("to_good_movement_id").nullable().uniqueIndex()
    val userId: Column<Int> = integer("user_id").uniqueIndex()
    val type: Column<String> = varchar("type", 24)
    val remark: Column<String?> = varchar("remark", 45).nullable()
    val createdAt: Column<LocalDateTime> = datetime("created_at")
}