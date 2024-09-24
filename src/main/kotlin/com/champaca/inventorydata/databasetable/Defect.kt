package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.math.BigDecimal
import java.time.LocalDateTime

object Defect: IntIdTable("cpc_defect") {
    val pileId = integer("cpc_pile_id")
    val manufacturingLineId: Column<Int> = integer("manufacturing_line_id").uniqueIndex()
    val skuId: Column<Int> = integer("sku_id")
    val type: Column<String> = varchar("type", 8)
    val qty : Column<BigDecimal> = decimal("qty", 5, 2)
    val createdAt : Column<LocalDateTime> = datetime("created_at")
    val status = varchar("status", 1)
}