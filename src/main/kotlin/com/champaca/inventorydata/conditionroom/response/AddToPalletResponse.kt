package com.champaca.inventorydata.conditionroom.response

import com.champaca.inventorydata.pile.PileError

sealed class AddToPalletResponse {
    data class Success(
        val pileCode: String
    ): AddToPalletResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): AddToPalletResponse()
}
