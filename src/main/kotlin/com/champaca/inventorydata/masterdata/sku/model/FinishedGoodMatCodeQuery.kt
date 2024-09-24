package com.champaca.inventorydata.masterdata.sku.model

data class FinishedGoodMatCodeQuery(
    val type: String, // e.g. flooring, decking, ending etc.
    val skuGroupCode: String, // e.g. R3, R4, R5, R7, R9
    val species: String,
    val fsc: Boolean,
    val thickness: String,
    val width: String,
    val length: String,
    val secondLayerSpecies: String?,
    val thirdLayerSpecies: String?,
    val fourthLayerSpecies: String?,
    val color: String?,
    val coating: String?,
    val accessories: String?,
    val pattern: String?,
    val texture: String?,
    val typeOfUse: String?,
    val flooringAccessories: String?,
    val stairStandardCode: String?,
    val furnitureCode: String?,
    val furniturePartNo: String?,
)