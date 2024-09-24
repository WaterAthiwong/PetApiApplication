package com.champaca.inventorydata.masterdata.manufacturingline.request

data class GetManufacturingLineRequest(
    @Deprecated("Use processTypeIds instead")
    val processTypeId: Int?,
    val processTypeIds: List<Int>?
)