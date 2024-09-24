package com.champaca.inventorydata.pile.request

data class UndoPileRequest(
    val pileCode: String,
    val reason: String
)
