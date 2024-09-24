package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.PileRelocation
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class PileRelocationDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<PileRelocationDao>(PileRelocation)
    var fromStoreLocationId by PileRelocation.fromStoreLocationId
    var toStoreLocationId by PileRelocation.toStoreLocationId
    var pileId by PileRelocation.pileId
    var userId by PileRelocation.userId
    var lotSet by PileRelocation.lotSet
    var createdAt by PileRelocation.createdAt
}