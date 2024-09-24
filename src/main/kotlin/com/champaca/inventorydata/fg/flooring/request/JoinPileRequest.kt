package com.champaca.inventorydata.fg.flooring.request

data class JoinPileRequest(
    val mainPileCode: String,
    val joinPileCodes: List<String>
)