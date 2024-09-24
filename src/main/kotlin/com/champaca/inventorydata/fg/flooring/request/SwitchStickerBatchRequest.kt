package com.champaca.inventorydata.fg.flooring.request

data class SwitchStickerBatchRequest(
    val batchId: Int,
    val codes: List<String>,
)
