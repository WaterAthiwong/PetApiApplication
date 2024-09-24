package com.champaca.inventorydata.fg.flooring.request

data class GetStickerDetailsRequest(
    val batchId: Int,
    val rangeFrom: Int?,
    val rangeTo: Int?,
    val codes: List<String> = emptyList()
)
