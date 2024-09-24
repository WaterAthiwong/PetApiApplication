package com.champaca.inventorydata.pile.response

import com.champaca.inventorydata.pile.PileError

sealed class StockCountResponse {
    data class Success(
        val success: Boolean = true
    ): StockCountResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): StockCountResponse()
}
