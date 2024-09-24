package com.champaca.inventorydata.conditionroom.response

import com.champaca.inventorydata.pile.PileError

sealed class AssembleResponse {
    data class Success(
        val pileCode: String,
    ): AssembleResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): AssembleResponse()
}
