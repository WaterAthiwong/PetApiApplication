package com.champaca.inventorydata.databasetable

import com.champaca.inventorydata.databasetable.columntype.ynBool
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object PileTransaction: IntIdTable("cpc_pile_transaction") {
    val pileId: Column<Int> = integer("cpc_pile_id").uniqueIndex()
    val fromGoodMovementId: Column<Int?> = integer("from_good_movement_id").nullable().uniqueIndex()
    val toGoodMovementId: Column<Int?> = integer("to_good_movement_id").nullable().uniqueIndex()
    val palletId: Column<Int?> = integer("cpc_pallet_id").nullable()
    val userId: Column<Int> = integer("user_id").uniqueIndex()
    val lotSet: Column<Int> = integer("lot_set")
    val type: Column<String> = varchar("type", 24)
    val fromPile: Column<String?> = varchar("from_cpc_pile", 256).nullable()
    val toPileId: Column<Int?> = integer("to_cpc_pile_id").nullable()
    val fromLotNos: Column<String?> = varchar("from_lot_nos", 256).nullable()
    val toLotNos: Column<String?> = varchar("to_lot_nos", 256).nullable()
    val movingQty: Column<String?> = varchar("moving_qty", 64).nullable()
    val remainingQty: Column<String?> = varchar("remaining_qty", 64).nullable()
    val remark: Column<String?> = varchar("remark", 45).nullable()
    val undone: Column<Boolean> = ynBool("undone")
    val createdAt: Column<LocalDateTime> = datetime("created_at")
}