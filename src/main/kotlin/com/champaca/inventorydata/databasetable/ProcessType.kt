package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object ProcessType: IntIdTable("process_type") {
    val prefix : Column<String> = varchar("prefix", 2)
    val name : Column<String> = varchar("name", 100)
    val erpCostCenterCode : Column<String> = varchar("erp_cost_center_code", 10)
    val erpCostCenterName : Column<String> = varchar("erp_cost_center_name", 45)
    val processDigit : Column<String> = varchar("process_digit", 1)
    val departmentId = reference("department_id", Department)
    val status : Column<String> = varchar("status", 1)
}