package com.champaca.inventorydata.model

import java.math.BigDecimal

interface AdditionalDataHolder {
    val data: Map<String, AdditionalDataRecord>

    fun hasData(name: String): Boolean = data.containsKey(name)

    fun getDataAsString(name: String, defaultValue: String): String

    fun getDataAsInt(name: String, defaultValue: Int): Int

    fun getDataAsDouble(name: String, defaultValue: Double): Double

    fun getDataAsBigDecimal(name: String, defaultValue: BigDecimal): BigDecimal
}