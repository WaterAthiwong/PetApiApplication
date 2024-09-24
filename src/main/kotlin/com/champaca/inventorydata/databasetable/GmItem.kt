package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.math.BigDecimal
import java.time.LocalDateTime

object GmItem: IntIdTable("gm_item") {
    val goodMovementId: Column<Int> = integer("good_movement_id")
    val lotNoId: Column<Int> = integer("lot_no_id")
    val skuId: Column<Int> = integer("sku_id")
    val storeLocationId: Column<Int> = integer("store_location_id")
    val gmItemId: Column<Int> = integer("gm_item_id")
    val qty : Column<BigDecimal> = decimal("qty", 10, 2)
    val remark : Column<String?> = text("remark").nullable()
    val createdAt : Column<LocalDateTime> = datetime("created_at")
    val status : Column<String> = varchar("status", 1)
}