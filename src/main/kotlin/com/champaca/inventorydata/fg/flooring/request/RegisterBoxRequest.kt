package com.champaca.inventorydata.fg.flooring.request

data class RegisterBoxRequest(
    val departmentPrefix: String,
    val boxCode: String,
    val goodMovementId: Int,
    val manufacturingLineId: Int?,
    val location: String
)