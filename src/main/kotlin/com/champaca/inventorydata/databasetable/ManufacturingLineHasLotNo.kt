package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.math.BigDecimal
import java.time.LocalDateTime

object ManufacturingLineHasLotNo: Table("manufacturing_line_has_lot_no") {
    val manufacturingLineId: Column<Int> = integer("manufacturing_line_id").uniqueIndex()
    val lotNoId: Column<Int> = integer("lot_no_id").uniqueIndex()
    val skuId: Column<Int> = integer("sku_id").uniqueIndex()
    val qty : Column<BigDecimal> = decimal("qty", 10, 2)
    val updatedAt : Column<LocalDateTime> = datetime("updated_at")
    override val primaryKey = PrimaryKey(manufacturingLineId, lotNoId, skuId)
}