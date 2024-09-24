package com.champaca.inventorydata.conditionroom.request

import com.champaca.inventorydata.pile.request.PileItem

data class TransferToProductionRequest(
    val goodMovementId: Int,
    val departmentPrefix: String,
    val items: List<PileItem>,
    val location: String,
    val orderNo: String?,
    val remark: String?
)