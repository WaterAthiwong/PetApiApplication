package com.champaca.inventorydata.masterdata.manufacturingline.model

data class ManufacturingLineData(
    val id: Int,
    val processTypeId: Int,
    val name: String,
    val processName: String,
    val prefix: String
)
