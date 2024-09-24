package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.ManufacturingLine
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ManufacturingLineDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<ManufacturingLineDao>(ManufacturingLine)
    var processTypeId by ManufacturingLine.processTypeId
    var name by ManufacturingLine.name
    var erpMachineCode by ManufacturingLine.erpMachineCode
    var status by ManufacturingLine.status
}