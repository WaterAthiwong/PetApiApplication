package com.champaca.inventorydata.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.math.BigDecimal

class AdditionalDataHolderImpl(val dataString: String): AdditionalDataHolder {

    override val data: Map<String, AdditionalDataRecord> = decodeJsonString(dataString)
    override fun getDataAsString(name: String, defaultValue: String): String {
        return if(hasData(name)) data.get(name)!!.value.content else defaultValue
    }

    override fun getDataAsInt(name: String, defaultValue: Int): Int {
        return if(hasData(name)) data.get(name)!!.value.content.toInt() else defaultValue
    }

    override fun getDataAsDouble(name: String, defaultValue: Double): Double {
        return if(hasData(name)) data.get(name)!!.value.content.toDouble() else defaultValue
    }

    override fun getDataAsBigDecimal(name: String, defaultValue: BigDecimal): BigDecimal {
        return if(hasData(name)) data.get(name)!!.value.content.toBigDecimal() else defaultValue
    }

    private fun decodeJsonString(dataString: String): Map<String, AdditionalDataRecord> {
        val listedData: List<AdditionalDataRecord> = Json.decodeFromString(dataString)
        return listedData.associateBy({it.name}, {it})
    }
}