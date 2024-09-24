package com.champaca.inventorydata.pallet.response

import com.champaca.inventorydata.pallet.PalletError
import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.model.PickedGoodMovement

sealed class PickPalletResponse {
    data class Success(
        val pileCount: Int,
        val goodMovement: PickedGoodMovement,
        val items: List<MovingItem>
    ): PickPalletResponse()

    data class Failure(
        val goodMovement: PickedGoodMovement?,
        val items: List<MovingItem>?,
        val errorType: PalletError,
        val errorMessage: String? = null
    ): PickPalletResponse()
}
