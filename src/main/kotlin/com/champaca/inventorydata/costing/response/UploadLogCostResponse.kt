package com.champaca.inventorydata.costing.response

import com.champaca.inventorydata.pile.PileError

sealed class UploadLogCostResponse{
    data class Success(
        val newPileNos: Int,
        val existingPileNos: Int
    ): UploadLogCostResponse()
    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): UploadLogCostResponse()
}