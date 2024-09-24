package com.champaca.inventorydata.log.model

import com.champaca.inventorydata.log.LogDeliveryError
import java.time.LocalDateTime

data class ReceivedLogIncidentData(
    val userId: Int,
    val logId: Int?,
    val barcode: String,
    val errorType: LogDeliveryError,
    var isSolved: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    var id: Int = -1
}