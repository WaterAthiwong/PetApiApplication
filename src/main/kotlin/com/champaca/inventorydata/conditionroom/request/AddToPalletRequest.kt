package com.champaca.inventorydata.conditionroom.request

import com.champaca.inventorydata.pile.request.PileItem

data class AddToPalletRequest(
    val pileCode: String,
    val items: List<PileItem> = emptyList(),
    val remark: String,
)