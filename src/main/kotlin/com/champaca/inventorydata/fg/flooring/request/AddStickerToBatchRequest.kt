package com.champaca.inventorydata.fg.flooring.request

data class AddStickerToBatchRequest(
    val batchId: Int,
    val qty: Int
)