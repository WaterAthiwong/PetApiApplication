package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.ProcessType
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ProcessTypeDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<ProcessTypeDao>(ProcessType)
    var prefix by ProcessType.prefix
    var name by ProcessType.name
    var departmentId by ProcessType.departmentId
    var status by ProcessType.status
}