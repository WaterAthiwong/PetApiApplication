package com.champaca.inventorydata.fg.flooring.response

import com.champaca.inventorydata.pile.PileError

sealed class CreateStickerBatchResponse {
    data class Success(
        val batchId: Int,
        val batchCode: String,
    ): CreateStickerBatchResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): CreateStickerBatchResponse()
}
