package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.SalesOrderLine
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SalesOrderLineDao(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<SalesOrderLineDao>(SalesOrderLine)
    var odooId by SalesOrderLine.odooId
    var name by SalesOrderLine.name
    var productId by SalesOrderLine.productId
    var qtyToDeliver by SalesOrderLine.qtyToDeliver
    var orderedQty by SalesOrderLine.orderedQty
    var createdAt by SalesOrderLine.createdAt
    var updatedAt by SalesOrderLine.updatedAt
}