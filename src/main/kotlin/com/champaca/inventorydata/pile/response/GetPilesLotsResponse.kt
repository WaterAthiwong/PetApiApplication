package com.champaca.inventorydata.pile.response

import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.model.PileEntry

sealed class GetPilesLotsResponse {
    data class Success(
        val piles: List<PileEntry>?
    ): GetPilesLotsResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String?
    ): GetPilesLotsResponse()
}