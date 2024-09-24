package com.champaca.inventorydata.databasetable.columntype

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.vendors.currentDialect

fun Table.ynBool(name: String): Column<Boolean> = registerColumn(name, YNBoolColumnType)

object YNBoolColumnType: ColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.booleanType()

    override fun valueFromDB(value: Any): Any = when (value) {
        "Y" -> true
        "N" -> false
        else -> error("Unexpected value of type Boolean: $value")
    }

    override fun notNullValueToDB(value: Any): Any = when (value) {
        true -> "Y"
        false -> "N"
        else -> error("Unexpected value of type Boolean: $value")
    }
}