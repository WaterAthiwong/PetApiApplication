package com.champaca.inventorydata.pile.request

data class ImportExistingPileRequest(
    val lotGroupCode: String,
    val monthPrefix: String,
    val processPrefix: String,
    val jobNo: String,
    val pileType: String = "woodPile",
    val remark: String?
)