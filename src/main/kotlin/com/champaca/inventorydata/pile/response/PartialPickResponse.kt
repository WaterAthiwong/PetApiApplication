package com.champaca.inventorydata.pile.response

import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.model.PickedGoodMovement

sealed class PartialPickResponse {
    data class Success(
        val pickedItems: List<MovingItem>,
        val remainingItems: List<MovingItem>
    ): PartialPickResponse()

    data class Failure(
        val goodMovement: PickedGoodMovement?,
        val errorType: PileError,
        val errorMessage: String? = null
    ): PartialPickResponse()
}
