package com.champaca.inventorydata.data.report.request

data class GetSawedLogRequest(
    val goodsMovementCodes: List<String>?,
    val startDate: String?,
    val endDate: String?
)