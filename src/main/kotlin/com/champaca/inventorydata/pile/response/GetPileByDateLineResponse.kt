package com.champaca.inventorydata.pile.response

import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.model.PileEntry

sealed class GetPileByDateLineResponse {
    data class Success(
        val piles: MutableList<PileEntry>?
    ): GetPileByDateLineResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String?
    ): GetPileByDateLineResponse()
}