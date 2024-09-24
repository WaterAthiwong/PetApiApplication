package com.champaca.inventorydata.fg.flooring.response

import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.model.PickedGoodMovement

sealed class RecordStickerPrintedResponse {
    data class Success(
        var count: Int
    ): RecordStickerPrintedResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): RecordStickerPrintedResponse()

}
