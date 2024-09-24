package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object ManufacturingLine: IntIdTable("manufacturing_line") {
    val processTypeId: Column<Int> = integer("process_type_id")
    val name : Column<String> = varchar("name", 100)
    val erpMachineCode: Column<String?> = varchar("erp_machine_code",5).nullable()
    val status : Column<String> = varchar("status", 1)
}