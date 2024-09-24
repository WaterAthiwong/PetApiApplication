package com.champaca.inventorydata.model

import kotlinx.serialization.json.JsonPrimitive

@kotlinx.serialization.Serializable
data class AdditionalDataRecord(val name: String, val value: JsonPrimitive, val label: String)