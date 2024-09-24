package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object ConditionRoomTransfer: IntIdTable("cpc_cond_room_transfer") {
    val transferGoodMovementId = integer("transfer_good_movement_id")
    val transferredLotNoId = integer("transferred_lot_no_id")
    val newLotNoId = integer("new_lot_no_id")
    val qty = decimal("qty", 10, 4)
    val createdAt = datetime("created_at")
}