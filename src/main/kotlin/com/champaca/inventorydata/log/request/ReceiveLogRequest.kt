package com.champaca.inventorydata.log.request

import com.champaca.inventorydata.log.StoredLogSearchParam

data class ReceiveLogRequest(
    val barcode: String,
    val receivedDate: String,
    val forestryBook: String?,
    val forestryBookNo: String?,
    val logDeliveryId: Int?,
    val location: String?
) {
    fun toStoredLogSearchParam(): StoredLogSearchParam {
        return StoredLogSearchParam(
            refCodes = listOf(barcode)
        )
    }
}