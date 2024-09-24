package com.champaca.inventorydata.sawmill.response

import com.champaca.inventorydata.log.model.StoredLog
import com.champaca.inventorydata.sawmill.SawMillError

sealed class CancelBookingResponse {
    data class Success(
        val logNoId: Int
    ): CancelBookingResponse()

    data class Failure(
        val errorType: SawMillError,
        val errorMessage: String = ""
    ): CancelBookingResponse()
}