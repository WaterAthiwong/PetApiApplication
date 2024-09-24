package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.StoreLocation
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class StoreLocationDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<StoreLocationDao>(StoreLocation)
    var storeZoneId by StoreLocation.storeZoneId
    var warehouseId by StoreLocation.warehouseId
    var code by StoreLocation.code
    var name by StoreLocation.name
    var status by StoreLocation.status
}