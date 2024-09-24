package com.champaca.inventorydata.fg.flooring.response

import com.champaca.inventorydata.goodmovement.GoodMovementError

sealed class SwitchStickerBatchResponse {
    data class Success(
        val codes: List<String>
    ): SwitchStickerBatchResponse()

    data class Failure(
        val errorType: GoodMovementError,
        val errorMessage: String? = null
    ): SwitchStickerBatchResponse()
}
