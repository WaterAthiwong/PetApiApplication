package com.champaca.inventorydata.conditionroom.response

import com.champaca.inventorydata.conditionroom.model.SuggestedShelf
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.model.MovingItem
import java.math.BigDecimal

sealed class TransferToStorageResponse {
    data class Success(
        val remainingItems: List<MovingItem>,
        val suggestedShelves: List<SuggestedShelf>,
        val thicknesses: List<BigDecimal>,
        val widths: List<BigDecimal>,
        val lengths: List<BigDecimal>,
        val grades: List<String>
    ): TransferToStorageResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): TransferToStorageResponse()

}
