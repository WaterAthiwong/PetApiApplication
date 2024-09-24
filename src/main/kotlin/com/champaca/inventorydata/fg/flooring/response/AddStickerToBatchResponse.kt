package com.champaca.inventorydata.fg.flooring.response

import com.champaca.inventorydata.pile.PileError

sealed class AddStickerToBatchResponse {
    data class Success(
        val success: Boolean
    ): AddStickerToBatchResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): AddStickerToBatchResponse()
}
