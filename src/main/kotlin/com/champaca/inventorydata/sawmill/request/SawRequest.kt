package com.champaca.inventorydata.sawmill.request

data class SawRequest(
    val barcode: String,
    val goodMovementId: Int,
    val manufacturingLineId: Int,
    val location: String?
)