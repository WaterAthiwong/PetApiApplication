package com.champaca.inventorydata.sawmill.request

data class GetManufacturedRequest(
    val goodsMovementCodes: List<String>?,
    val manufacturingLines: List<String>?,
    val startDate: String?,
    val endDate: String?
)