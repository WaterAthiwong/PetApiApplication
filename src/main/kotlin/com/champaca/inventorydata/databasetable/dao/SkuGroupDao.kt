package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.SkuGroup
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SkuGroupDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<SkuGroupDao>(SkuGroup)
    var name by SkuGroup.name
    var erpMainGroupId by SkuGroup.erpMainGroupId
    var erpMainGroupName by SkuGroup.erpMainGroupName
    var erpGroupCode by SkuGroup.erpGroupCode
    var erpGroupName by SkuGroup.erpGroupName
    var status by SkuGroup.status
}