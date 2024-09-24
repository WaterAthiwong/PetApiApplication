package com.champaca.inventorydata.pile.response

import com.champaca.inventorydata.pile.PileError

sealed class EditPileResponse {
    data class Success(
        val removedItems: Int,
        val addedItems: Int
    ): EditPileResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): EditPileResponse()
}
