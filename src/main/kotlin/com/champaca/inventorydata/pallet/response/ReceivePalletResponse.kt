package com.champaca.inventorydata.pallet.response

import com.champaca.inventorydata.pallet.PalletError
import com.champaca.inventorydata.pile.model.MovingItem

sealed class ReceivePalletResponse {
    data class Success(
        val receivingItems: List<MovingItem>
    ): ReceivePalletResponse()

    data class Failure(
        val errorType: PalletError,
        val errorMessage: String? = null
    ): ReceivePalletResponse()
}
