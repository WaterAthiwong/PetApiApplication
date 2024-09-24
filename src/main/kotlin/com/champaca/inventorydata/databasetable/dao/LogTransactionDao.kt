package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.LogTransaction
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class LogTransactionDao(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<LogTransactionDao>(LogTransaction) {
        const val BOOK = "book"
        const val CANCEL_BOOKING = "cancelBooking"
        const val SAW = "saw"
    }

    var lotNoId by LogTransaction.lotNoId
    var fromGoodMovementId by LogTransaction.fromGoodMovementId
    var toGoodMovementId by LogTransaction.toGoodMovementId
    var userId by LogTransaction.userId
    var type by LogTransaction.type
    var remark by LogTransaction.remark
    var createdAt by LogTransaction.createdAt
}