package com.champaca.inventorydata.pile.response

import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.model.MovingItem

sealed class TransferResponse {
    data class Success(
        val pileCode: String,
        val pileId: Int,
        val remainingItems: List<MovingItem>,
        val blankPile: Boolean
    ): TransferResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): TransferResponse()
}
