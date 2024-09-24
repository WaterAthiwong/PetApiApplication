package com.champaca.inventorydata.databasetable

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.time.LocalDateTime

object Pile: IntIdTable("cpc_pile") {
    val format = Json { prettyPrint = true }

    val manufacturingLineId: Column<Int?> = integer("manufacturing_line_id").nullable()
    val goodMovementId = reference("good_movement_id", GoodMovement)
    val originGoodMovementId = integer("origin_good_movement_id")
    val storeLocationId = integer("store_location_id")
    val palletId = reference("cpc_pallet_id", Pallet).nullable()
    val code: Column<String> = varchar("code", 15)
    val processTypePrefix: Column<String> = varchar("process_type_prefix", 2)
    val lotSet: Column<Int> = integer("lot_set")
    val type: Column<String> = varchar("type", 8)
    val orderNo: Column<String?> = varchar("order_no", 64).nullable()
    val remark: Column<String?> = varchar("remark", 255).nullable()
    val extraAttributes: Column<Map<String, String>?> = json<Map<String, String>>("extra_attributes", format).nullable()
    val printedAt: Column<LocalDateTime> = datetime("printed_at")
    val countedAt: Column<LocalDateTime?> = datetime("counted_at").nullable()
    val countedUserId: Column<Int?> = integer("counted_user_id").nullable()
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val updatedAt: Column<LocalDateTime> = datetime("updated_at")
    val status: Column<String> = varchar("status", 1)
}