package com.champaca.inventorydata.goodmovement.request

data class AddReferenceRequest(
    val goodReceiptGoodMovementId: Int,
    val pickingOrderGoodMovementIds: List<Int>
) {
}