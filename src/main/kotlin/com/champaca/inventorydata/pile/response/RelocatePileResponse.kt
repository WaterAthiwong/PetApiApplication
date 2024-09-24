package com.champaca.inventorydata.pile.response

import com.champaca.inventorydata.pile.PileError

sealed class RelocatePileResponse {
    data class Success(
        val count: Int
    ): RelocatePileResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): RelocatePileResponse()
}