package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.math.BigDecimal
import java.time.LocalDateTime

object StoreLocationHasLotNo: Table("store_location_has_lot_no") {
    val storeLocationId = reference("store_location_id", StoreLocation)
    val lotNoId = reference("lot_no_id", LotNo)
    val skuId: Column<Int> = integer("sku_id").uniqueIndex()
    val qty : Column<BigDecimal> = decimal("qty", 10, 2)
    val updatedAt : Column<LocalDateTime> = datetime("updated_at")
    override val primaryKey = PrimaryKey(storeLocationId, lotNoId, skuId)
}