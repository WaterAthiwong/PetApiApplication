package com.champaca.inventorydata.databasetable

import com.champaca.inventorydata.databasetable.GmItem.nullable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.math.BigDecimal
import java.time.LocalDateTime

object Log: IntIdTable("cpc_log") {
    val logDeliveryId: Column<Int> = integer("cpc_log_delivery_id")
    val receivingUserId: Column<Int?> = integer("receiving_user_id").nullable()
    val exportingUserId: Column<Int?> = integer("exporting_user_id").nullable()
    val goodsMovementId: Column<Int?> = integer("good_movement_id").nullable()
    val storeLocationId: Column<Int?> = integer("store_location_id").nullable()
    val itemNo: Column<Int> = integer("item_no")
    val batchNo: Column<String> = varchar("batch_no", 16)
    val species: Column<String> = varchar("species", 2)
    val length: Column<Int> = integer("length")
    val circumference: Column<Int> = integer("circumference")
    val volumnM3: Column<BigDecimal> = decimal("volumn_m3", 4, 3)
    val logNo: Column<String> = varchar("log_no", 6)
    val matCode : Column<String> = varchar("mat_code", 45)
    val refCode: Column<String> = varchar("ref_code", 12)
    val errorCode: Column<Int?> = integer("error_code").nullable()
    val receivedAt: Column<LocalDateTime?> = datetime("received_at").nullable()
    val exportedToWmsAt: Column<LocalDateTime?> = datetime("exported_to_wms_at").nullable()
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val updatedAt: Column<LocalDateTime> = datetime("updated_at")
    val status: Column<String> = varchar("status", 1)
}