package com.champaca.inventorydata.log.response

import com.champaca.inventorydata.log.LogDeliveryError

sealed class RecheckLogIncidentResponse {
    data class Success(
        val successBarcodes: List<String>,
        val failedBarcodes: List<String>
    ): RecheckLogIncidentResponse()

    data class Failure(
        val errorType: LogDeliveryError
    ): RecheckLogIncidentResponse()
}