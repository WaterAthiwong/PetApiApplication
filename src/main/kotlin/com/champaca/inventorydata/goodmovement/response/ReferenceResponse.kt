package com.champaca.inventorydata.goodmovement.response

import com.champaca.inventorydata.goodmovement.GoodMovementError

sealed class ReferenceResponse {
    data class Success(
        val success: Boolean = true
    ): ReferenceResponse()

    data class Failure(
        val errorType: GoodMovementError,
        val errorMessage: String? = null
    ): ReferenceResponse()
}
