package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object PileHasLotNo: IntIdTable("cpc_pile_has_lot_no") {
    val pileId = reference("cpc_pile_id", Pile)
    val lotNoId = reference("lot_no_id", LotGroup)
    val transferredGoodMovementId = reference("transferred_good_movement_id", GoodMovement).nullable()
    val transferredLotNoId = reference("transferred_lot_no_id", LotNo).nullable()
    val transferredQty = decimal("transferred_qty", 10, 2).nullable()
    val lotSet: Column<Int> = integer("lot_set")
    val createdAt: Column<LocalDateTime> = datetime("created_at")
}