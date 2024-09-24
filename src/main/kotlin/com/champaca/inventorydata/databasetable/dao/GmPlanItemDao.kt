package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.GmItem
import com.champaca.inventorydata.databasetable.GmPlanItem
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class GmPlanItemDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<GmPlanItemDao>(GmPlanItem)
    var goodMovementId by GmPlanItem.goodMovementId
    var skuId by GmPlanItem.skuId
    var qty by GmPlanItem.qty
    var remark by GmPlanItem.remark
    var status by GmPlanItem.status
}