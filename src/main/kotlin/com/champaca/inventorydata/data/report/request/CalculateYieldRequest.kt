package com.champaca.inventorydata.data.report.request

data class CalculateYieldRequest(
    val departmentId: Int,
    val fromDate: String,
    val toDate: String,
    val excludeWastedOutput: Boolean = true
)