package com.champaca.inventorydata.pile.response

import com.champaca.inventorydata.pile.PileError

sealed class RemovePileResponse {
    data class Success(
        val pileCode: String
    ): RemovePileResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): RemovePileResponse()
}
