package com.champaca.inventorydata.pile.request

data class EditPileRequest(
    val pileCode: String,
    val toBeRemovedLotNos: List<Int>?,
    val toBeAddedItems: List<PileItem>?,
    val orderNo: String?,
    val refNo: String?,
    val customer: String?,
    val remark: String?,
    val reason: String
)
