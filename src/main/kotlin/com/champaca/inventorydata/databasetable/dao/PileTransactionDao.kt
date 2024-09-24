package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.PileTransaction
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class PileTransactionDao(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<PileTransactionDao>(PileTransaction) {
        const val PICK = "pick"
        const val PARTIAL_PICK = "partialPick"
        const val PICK_FOR_ASSEMBLE = "pickForAssemble"
        const val RECEIVE = "receive"
        const val CREATE = "create"
        const val EDIT = "edit"
        const val REMOVE = "remove"
        const val UNDO = "undo"
        const val TRANSFER = "transfer"
        const val RECEIVE_TRANSFER = "receiveTransfers"
        const val ASSEMBLE = "assemble"
    }
    var pileId by PileTransaction.pileId
    var fromGoodMovementId by PileTransaction.fromGoodMovementId
    var toGoodMovementId by PileTransaction.toGoodMovementId
    var userId by PileTransaction.userId
    var lotSet by PileTransaction.lotSet
    var type by PileTransaction.type
    var fromPile by PileTransaction.fromPile
    var toPileId by PileTransaction.toPileId
    var fromLotNos by PileTransaction.fromLotNos
    var toLotNos by PileTransaction.toLotNos
    var movingQty by PileTransaction.movingQty
    var remainingQty by PileTransaction.remainingQty
    var remark by PileTransaction.remark
    var undone by PileTransaction.undone
    var createdAt by PileTransaction.createdAt
}