package com.champaca.inventorydata.conditionroom.request

import com.champaca.inventorydata.pile.request.PileItem

data class AssembleRequest(
    val location: String,
    val pickDetails: List<PickDetail>,
    val pickingGoodMovementId: Int?,
    val departmentPrefix: String,
    val orderNo: String?
) {
    data class PickDetail(
        val fromPileCode: String,
        val items: List<PileItem>
    )
}