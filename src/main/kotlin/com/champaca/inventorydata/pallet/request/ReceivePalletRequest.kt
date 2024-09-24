package com.champaca.inventorydata.pallet.request

data class ReceivePalletRequest(
    val palletCode: String,
    val skuGroupCode: String, // e.g. R3, R4, R5, R7, R9
    val goodMovementId: Int,
    val manufacturingLineId: Int,
    val processPrefix: String,
    val location: String,
)
