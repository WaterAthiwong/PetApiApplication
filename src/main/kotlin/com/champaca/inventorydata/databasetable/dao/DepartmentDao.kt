package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.Department
import com.champaca.inventorydata.databasetable.GmItem
import com.champaca.inventorydata.databasetable.GmPlanItem
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class DepartmentDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<DepartmentDao>(Department)
    var name by Department.name
    var status by Department.status
}