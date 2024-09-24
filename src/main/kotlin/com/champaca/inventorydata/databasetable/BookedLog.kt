package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object BookedLog: IntIdTable("cpc_booked_log") {
    val lotNoId = reference("lot_no_id", LotNo)
    val goodMovementId = reference("good_movement_id", GoodMovement)
    val status = varchar("status", 1)
}