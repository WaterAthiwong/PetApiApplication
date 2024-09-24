package com.champaca.inventorydata.data.report.request

data class GetPileTransactionRequest(
    val departmentId: Int?,
    val fromProductionDate: String?,
    val toProductionDate: String?,
    val fromTransactionDate: String?,
    val toTransactionDate: String?,
    val code: String?
)