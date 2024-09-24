package com.champaca.inventorydata.pile.response

import com.champaca.inventorydata.pile.PileError

sealed class CreatePileResponse {
    data class Success(
        val pileCode: String,
        val pileCodes: List<String>
    ): CreatePileResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): CreatePileResponse()
}
