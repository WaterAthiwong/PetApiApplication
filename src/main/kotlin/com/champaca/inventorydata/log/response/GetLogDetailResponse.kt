package com.champaca.inventorydata.log.response

import com.champaca.inventorydata.log.usecase.GetLogDetailStatus
import com.champaca.inventorydata.log.model.StoredLog

sealed class GetLogDetailResponse {
    data class Success(
        val log: StoredLog,
        val status: GetLogDetailStatus
    ): GetLogDetailResponse()
    data class Failure(val errorType: GetLogDetailStatus): GetLogDetailResponse()
}