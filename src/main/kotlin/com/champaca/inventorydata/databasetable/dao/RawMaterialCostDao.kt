package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.RawMaterialCost
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class RawMaterialCostDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<RawMaterialCostDao>(RawMaterialCost) {
        const val LOG = "log"
        const val SAWN_TIMBER = "sawnTimber"
    }

    var supplierId by RawMaterialCost.supplierId
    var skuId by RawMaterialCost.skuId
    var type by RawMaterialCost.type
    var poNo by RawMaterialCost.poNo
    var deliveryCycle by RawMaterialCost.deliveryCycle
    var unitCostM3 by RawMaterialCost.unitCostM3
    var unitCostFt3 by RawMaterialCost.unitCostFt3
    var createdAt by RawMaterialCost.createdAt
    var updatedAt by RawMaterialCost.updatedAt
    var status by RawMaterialCost.status
}