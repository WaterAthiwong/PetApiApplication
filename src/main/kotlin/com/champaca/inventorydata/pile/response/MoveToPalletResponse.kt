package com.champaca.inventorydata.pile.response

import com.champaca.inventorydata.pile.PileError

sealed class MoveToPalletResponse {
    data class Success(
        val count: Int
    ): MoveToPalletResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): MoveToPalletResponse()
}