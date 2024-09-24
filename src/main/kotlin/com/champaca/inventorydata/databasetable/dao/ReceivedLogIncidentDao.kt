package com.champaca.inventorydata.databasetable.dao


import com.champaca.inventorydata.databasetable.ReceivedLogIncident
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ReceivedLogIncidentDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<ReceivedLogIncidentDao>(ReceivedLogIncident)
    var userId by ReceivedLogIncident.userId
    var logId by ReceivedLogIncident.logId
    var barcode by ReceivedLogIncident.barcode
    var errorCode by ReceivedLogIncident.errorCode
    var isSolved by ReceivedLogIncident.isSolved
    var createdAt by ReceivedLogIncident.createdAt
    var updatedAt by ReceivedLogIncident.updatedAt
}