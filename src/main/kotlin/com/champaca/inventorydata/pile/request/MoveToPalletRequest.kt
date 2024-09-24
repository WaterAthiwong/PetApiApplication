package com.champaca.inventorydata.pile.request

data class MoveToPalletRequest(
    val palletCode: String,
    val pileCodes: List<String>
)