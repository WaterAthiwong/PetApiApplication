package com.champaca.inventorydata.pile.response

import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.model.PickedGoodMovement

sealed class PickPileResponse {
    data class Success(
        val goodMovement: PickedGoodMovement,
        val items: List<MovingItem>,
        var itemCount: Int,
        var pileCount: Int
    ): PickPileResponse()

    data class Failure(
        val goodMovement: PickedGoodMovement?,
        val items: List<MovingItem>?,
        val errorType: PileError,
        val errorMessage: String? = null
    ): PickPileResponse()

}
