package com.champaca.inventorydata.pile.response

import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.PileError

sealed class ReceivePileResponse {
    data class Success(
        val receivingItems: List<MovingItem>,
        val itemCount: Int,
        val pileCount: Int
    ): ReceivePileResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): ReceivePileResponse()
}
