package com.champaca.inventorydata.conditionroom.response

import com.champaca.inventorydata.conditionroom.model.SuggestedShelf
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.request.PileItem

sealed class GetRemainingItemsResponse {
    data class Success(
        val remainingItems: List<PileItem>,
        val suggestedShelves: List<SuggestedShelf>
    ): GetRemainingItemsResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): GetRemainingItemsResponse()

}
