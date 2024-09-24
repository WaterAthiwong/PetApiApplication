package com.champaca.inventorydata.sawmill.response

import com.champaca.inventorydata.log.model.StoredLog
import com.champaca.inventorydata.sawmill.SawMillError

sealed class SawResponse {
    data class Success(
        val storedLog: StoredLog,
        val itemCount: Int
    ): SawResponse()

    data class Failure(
        val errorType: SawMillError,
        val errorMessage: String = ""
    ): SawResponse()
}