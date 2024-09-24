package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.LotGroup
import com.champaca.inventorydata.databasetable.LotNo
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class LotGroupDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<LotGroupDao>(LotGroup)
    var code by LotGroup.code
    var lotGroupId by LotGroup.lotGroupId
    var refCode by LotGroup.refCode
    var extraAttributes by LotGroup.extraAttributes
    var createdAt by LotGroup.createdAt
    var updatedAt by LotGroup.updatedAt
    var status by LotGroup.status
    val lotNos by LotNoDao referrersOn LotNo.lotGroupId
}