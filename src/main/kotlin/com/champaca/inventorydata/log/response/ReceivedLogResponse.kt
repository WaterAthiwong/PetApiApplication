package com.champaca.inventorydata.log.response

import com.champaca.inventorydata.log.LogDeliveryError
import com.champaca.inventorydata.log.model.UploadedLog

sealed class ReceivedLogResponse {
    data class Success(
        val log: UploadedLog,
        val forestryBook: String?,
        val forestryBookNo: String?,
        val logDeliveryId: Int?,
        val stat: Stat
    ): ReceivedLogResponse()
    data class Failure(
        val type: LogDeliveryError,
        val errorCode: Int,
        val needToMark: Boolean,
        val log: UploadedLog?,
        val forestryBook: String?,
        val forestryBookNo: String?,
        val logDeliveryId: Int?,
        val stat: Stat
    ): ReceivedLogResponse()

    data class Stat(
        val receivedByForestryBook: Int,
        val totalByForestryBook: Int,
        val nonExistingBarcodes: Int
    )
}