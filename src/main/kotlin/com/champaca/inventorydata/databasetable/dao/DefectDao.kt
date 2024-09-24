package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.Defect
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class DefectDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<DefectDao>(Defect) {
        const val REJECTED = "rejected"
        const val REWORK = "rework"
    }

    var pileId by Defect.pileId
    var manufacturingLineId by Defect.manufacturingLineId
    var skuId by Defect.skuId
    var type by Defect.type
    var qty by Defect.qty
    var createdAt by Defect.createdAt
    var status by Defect.status
}