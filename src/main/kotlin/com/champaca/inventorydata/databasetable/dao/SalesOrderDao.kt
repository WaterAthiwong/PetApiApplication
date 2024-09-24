package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.SalesOrder
import com.champaca.inventorydata.databasetable.SalesOrderLine
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SalesOrderDao(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<SalesOrderDao>(SalesOrder)
    var odooId by SalesOrder.odooId
    var name by SalesOrder.name
    var partnerId by SalesOrder.partnerId
    var supplierId by SalesOrder.supplierId
    var warehouseId by SalesOrder.warehouseId
    var state by SalesOrder.state
    var deliveryState by SalesOrder.deliveryState
    var orderedAt by SalesOrder.orderedAt
    var createdAt by SalesOrder.createdAt
    var updatedAt by SalesOrder.updatedAt
    val lines by SalesOrderLineDao referrersOn SalesOrderLine.salesOrderId
}