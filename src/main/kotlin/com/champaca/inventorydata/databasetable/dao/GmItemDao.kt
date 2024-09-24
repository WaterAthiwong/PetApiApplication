package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.GmItem
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class GmItemDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<GmItemDao>(GmItem)
    var goodMovementId by GmItem.goodMovementId
    var lotNoId by GmItem.lotNoId
    var skuId by GmItem.skuId
    var storeLocationId by GmItem.storeLocationId
    var gmItemId by GmItem.gmItemId
    var qty by GmItem.qty
    var remark by GmItem.remark
    var createdAt by GmItem.createdAt
    var status by GmItem.status
}