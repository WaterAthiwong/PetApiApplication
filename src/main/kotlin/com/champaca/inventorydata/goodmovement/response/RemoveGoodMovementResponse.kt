package com.champaca.inventorydata.goodmovement.response

import com.champaca.inventorydata.goodmovement.GoodMovementError

sealed class RemoveGoodMovementResponse {
    data class Success(
        val id: Int
    ): RemoveGoodMovementResponse()

    data class Failure(
        val errorType: GoodMovementError,
        val errorMessage: String? = null
    ): RemoveGoodMovementResponse()
}
