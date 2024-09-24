package com.champaca.inventorydata.fg.flooring.response

import com.champaca.inventorydata.pile.PileError

sealed class EditStickerQtyResponse {
    data class Success(
        val success: Boolean
    ): EditStickerQtyResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): EditStickerQtyResponse()
}
