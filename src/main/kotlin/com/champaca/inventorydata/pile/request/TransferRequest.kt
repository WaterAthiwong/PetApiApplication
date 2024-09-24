package com.champaca.inventorydata.pile.request

data class TransferRequest(
    val fromPileCode: String,
    val toPileCode: String,
    val toDepartmentPrefix: String,
    val items: List<PileItem>
)