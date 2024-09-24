package com.champaca.inventorydata.goodmovement.response

import com.champaca.inventorydata.goodmovement.GoodMovementError

sealed class CreateWholeProductLineGoodMovementResponse {
    data class Success(
        val count: Int
    ): CreateWholeProductLineGoodMovementResponse()

    data class Failure(
        val errorType: GoodMovementError,
        val errorMessage: String? = null
    ): CreateWholeProductLineGoodMovementResponse()
}
