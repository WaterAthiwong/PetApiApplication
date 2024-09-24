package com.champaca.inventorydata.pile.request

data class ReceivePileRequest(
    val pileCode: String,
    val skuGroupCode: String, // e.g. R3, R4, R5, R7, R9
    val goodMovementId: Int,
    val manufacturingLineId: Int?,
    val processPrefix: String,
    val location: String,
    val receiveType: String // process or transfer
) {
    companion object {
        const val PROCESS = "process"
        const val TRANSFER = "transfer"
    }
}