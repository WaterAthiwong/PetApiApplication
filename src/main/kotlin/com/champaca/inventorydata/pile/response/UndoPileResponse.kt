package com.champaca.inventorydata.pile.response

import com.champaca.inventorydata.pile.PileError

sealed class UndoPileResponse {
    data class Success(
        val pileCode: String
    ): UndoPileResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): UndoPileResponse()
}
