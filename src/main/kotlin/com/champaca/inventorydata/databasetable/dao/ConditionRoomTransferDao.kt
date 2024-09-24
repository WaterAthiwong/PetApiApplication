package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.ConditionRoomTransfer
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ConditionRoomTransferDao(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<ConditionRoomTransferDao>(ConditionRoomTransfer)
    var transferGoodMovementId by ConditionRoomTransfer.transferGoodMovementId
    var transferredLotNoId by ConditionRoomTransfer.transferredLotNoId
    var newLotNoId by ConditionRoomTransfer.newLotNoId
    var qty by ConditionRoomTransfer.qty
    var createdAt by ConditionRoomTransfer.createdAt
}