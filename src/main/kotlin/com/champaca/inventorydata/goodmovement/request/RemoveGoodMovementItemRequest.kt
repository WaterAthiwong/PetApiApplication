package com.champaca.inventorydata.goodmovement.request

data class RemoveGoodMovementItemRequest(
    val goodMovememntItemIds: List<Int>,
    val reason: String,
    val type: String // This can be either "log" or "pilePartialPick"
)