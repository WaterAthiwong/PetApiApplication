package com.champaca.inventorydata.goodmovement.response

import com.champaca.inventorydata.goodmovement.GoodMovementError
import com.champaca.inventorydata.goodmovement.model.GoodMovementData

sealed class CreateGoodMovementResponse {
    data class Success(
        val goodMovement: GoodMovementData
    ): CreateGoodMovementResponse()

    data class Failure(
        val errorType: GoodMovementError,
        val errorMessage: String? = null
    ): CreateGoodMovementResponse()
}
