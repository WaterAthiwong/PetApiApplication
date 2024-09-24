package com.champaca.inventorydata.fg.flooring.response

import com.champaca.inventorydata.pile.PileError

sealed class JoinPileResponse {
    data class Success(
        val success: Boolean
    ): JoinPileResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): JoinPileResponse()
}
