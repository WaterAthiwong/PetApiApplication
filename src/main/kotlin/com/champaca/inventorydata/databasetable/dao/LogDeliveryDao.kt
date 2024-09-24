package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.LogDelivery
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class LogDeliveryDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<LogDeliveryDao>(LogDelivery)
    var supplierId by LogDelivery.supplierId
    var poNo by LogDelivery.poNo
    var deliveryRound by LogDelivery.deliveryRound
    var forestryBook by LogDelivery.forestryBook
    var forestryBookNo by LogDelivery.forestryBookNo
    var lotNo by LogDelivery.lotNo
    var fsc by LogDelivery.fsc
    var invoiceNo by LogDelivery.invoiceNo
    var fscNo by LogDelivery.fscNo
    var createdAt by LogDelivery.createdAt
    var status by LogDelivery.status
}