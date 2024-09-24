package com.champaca.inventorydata.sawmill.request

data class CancelBookingRequest(
    val barcode: String,
    val reason: String
)
