package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import java.math.BigDecimal

object GmPlanItem: IntIdTable("gm_plan_item") {
    val goodMovementId: Column<Int> = integer("good_movement_id")
    val skuId: Column<Int> = integer("sku_id")
    val qty : Column<BigDecimal> = decimal("qty", 10, 2)
    val remark : Column<String> = text("remark")
    val status : Column<String> = varchar("status", 1)
}