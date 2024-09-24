package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.PileHasLotNo
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class PileHasLotNoDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<PileHasLotNoDao>(PileHasLotNo)
    var pileId by PileHasLotNo.pileId
    var lotNoId by PileHasLotNo.lotNoId
    var transferredGoodMovementId by PileHasLotNo.transferredGoodMovementId
    var transferredLotNoId by PileHasLotNo.transferredLotNoId
    var transferredQty by  PileHasLotNo.transferredQty
    var lotSet by PileHasLotNo.lotSet
    var createdAt by PileHasLotNo.createdAt
}