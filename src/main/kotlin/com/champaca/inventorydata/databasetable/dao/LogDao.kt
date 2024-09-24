package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.Log
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class LogDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<LogDao>(Log)
    var logDeliveryId by Log.logDeliveryId
    var receivingUserId by Log.receivingUserId
    var exportingUserId by Log.exportingUserId
    var goodsMovementId by Log.goodsMovementId
    var storeLocationId by Log.storeLocationId
    var itemNo by Log.itemNo
    var batchNo by Log.batchNo
    var species by Log.species
    var length by Log.length
    var circumference by Log.circumference
    var volumnM3 by Log.volumnM3
    var logNo by Log.logNo
    var matCode  by Log.matCode
    var refCode by Log.refCode
    var errorCode by Log.errorCode
    var receivedAt by Log.receivedAt
    var exportedToWmsAt by Log.exportedToWmsAt
    var createdAt by Log.createdAt
    var updatedAt by Log.updatedAt
    var status by Log.status
}