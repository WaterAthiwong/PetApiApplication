package com.champaca.inventorydata.conditionroom.request

import com.champaca.inventorydata.pile.request.PileItem

data class TransferToStorageRequest(
    val goodMovementId: Int,
    val toPileCode: String,
    val departmentPrefix: String,
    val items: List<PileItem>,
)