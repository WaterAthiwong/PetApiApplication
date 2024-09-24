package com.champaca.inventorydata.masterdata.sku.model

data class MatCodeQuery(
    var mainGroup: String = "RM", // Other values are COMPONENT, HFG, TFG, FG
    val skuGroupCode: String, // e.g. R3, R4, R5, R7, R9
    val species: String,
    val fsc: Boolean,
    val thickness: String,
    val width: String,
    val length: String,
    val grade: String?,
    val qty: Int = -1,
    val secondLayerSpecies: String?,
    val thirdLayerSpecies: String?,
    val fourthLayerSpecies: String?
)