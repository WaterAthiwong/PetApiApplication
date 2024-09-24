package com.champaca.inventorydata.log.response

import com.champaca.inventorydata.log.LogDeliveryError

sealed class CreateLogsInWmsResponse {
    data class Success(
        val count: Int
    ): CreateLogsInWmsResponse()

    class Failure(
        val errorType: LogDeliveryError
    ): CreateLogsInWmsResponse()
}