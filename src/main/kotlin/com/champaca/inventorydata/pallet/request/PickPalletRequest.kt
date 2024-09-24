package com.champaca.inventorydata.pallet.request

data class PickPalletRequest(
    val palletCode: String,
    val goodMovementId: Int,
    val manufacturingLineId: Int?
)