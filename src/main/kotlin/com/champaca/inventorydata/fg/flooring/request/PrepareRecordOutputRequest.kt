package com.champaca.inventorydata.fg.flooring.request

data class PrepareRecordOutputRequest(
    val goodMovementId: Int,
    val processTypeId: Int,
    val manufacturingLineId: Int,
    val pileCode: String
)