package com.champaca.inventorydata.fg.flooring.response

import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.model.PickedGoodMovement

sealed class RegisterBoxResponse {
    data class Success(
        var boxCount: Int,
        var itemCount: Int
    ): RegisterBoxResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): RegisterBoxResponse()

}
