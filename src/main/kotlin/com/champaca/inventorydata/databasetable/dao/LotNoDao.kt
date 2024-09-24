package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.LotNo
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class LotNoDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<LotNoDao>(LotNo)
    var lotGroupId by LotNo.lotGroupId
    var code by LotNo.code
    var refCode by LotNo.refCode
    var additionalField by LotNo.additionalField
    var createdAt by LotNo.createdAt
    var updatedAt by LotNo.updatedAt
    var status by LotNo.status
    var extraAttributes by LotNo.extraAttributes
    var lotGroup by LotGroupDao referencedOn LotNo.lotGroupId
}