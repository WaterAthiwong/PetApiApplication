package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object PileRelocation: IntIdTable("cpc_pile_relocation") {
    val fromStoreLocationId: Column<Int> = integer("from_store_location_id")
    val toStoreLocationId: Column<Int> = integer("to_store_location_id")
    val pileId: Column<Int> = integer("cpc_pile_id")
    val palletId: Column<Int?> = integer("cpc_pallet_id").nullable()
    val userId: Column<Int> = integer("user_id")
    val lotSet: Column<Int> = integer("lot_set")
    val createdAt: Column<LocalDateTime> = datetime("created_at")
}