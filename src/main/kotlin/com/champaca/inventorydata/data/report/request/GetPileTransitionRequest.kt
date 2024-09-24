package com.champaca.inventorydata.data.report.request

data class GetPileTransitionRequest(
    val fromTransitionDate: String?,
    val toTransitionDate: String?,
    val fromDepartmentId: Int,
    val toDepartmentId: Int
)