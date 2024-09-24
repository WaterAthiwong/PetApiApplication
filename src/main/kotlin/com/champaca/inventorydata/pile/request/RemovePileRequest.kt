package com.champaca.inventorydata.pile.request

data class RemovePileRequest(
    val pileCode: String,
    val reason: String
)