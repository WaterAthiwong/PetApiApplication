package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object FinishedGoodSticker: IntIdTable("cpc_fg_sticker") {
    val batchId: Column<Int> = integer("cpc_fg_sticker_batch_id")
    val pileId: Column<Int?> = integer("cpc_pile_id").nullable()
    val code: Column<String> = varchar("code", 12)
    val isFragment: Column<Boolean> = bool("is_fragment")
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val updatedAt: Column<LocalDateTime> = datetime("updated_at")
    val printedAt: Column<LocalDateTime?> = datetime("printed_at").nullable()
    val status: Column<String> = varchar("status", 1)
}