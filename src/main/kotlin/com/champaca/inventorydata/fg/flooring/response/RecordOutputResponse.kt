package com.champaca.inventorydata.fg.flooring.response

import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.PileError

sealed class RecordOutputResponse {
    data class Success(
        val receivingItems: List<MovingItem>,
        val itemCount: Int,
        val pileCount: Int,
        val reworkPileCode: String?
    ): RecordOutputResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): RecordOutputResponse()
}
