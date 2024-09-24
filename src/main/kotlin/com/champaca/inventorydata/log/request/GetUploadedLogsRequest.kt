package com.champaca.inventorydata.log.request

import com.champaca.inventorydata.log.LogDeliverySearchParams
import com.champaca.inventorydata.log.LogSearchParams

data class GetUploadedLogsRequest(
    val receivedFrom: String?,
    val receivedTo: String?,
    val uploadedFrom: String?,
    val uploadedTo: String?,
    val poNo: String?,
    val deliveryRound: Int?,
    val supplierId: Int?,
    val lotNo: String?,
    val forestryBook: String?,
    val forestryBookNo: String?,
    val invoiceNo: String?,
    val fscNo: String?
) {
    fun toLogSearchParams(): LogSearchParams {
        return LogSearchParams(
            receivedFrom = receivedFrom,
            receivedTo = receivedTo
        )
    }

    fun toLogDeliverySearchParams(): LogDeliverySearchParams {
        return LogDeliverySearchParams(
            poNo = this.poNo,
            supplierId = this.supplierId,
            deliveryRound = this.deliveryRound,
            lotNo = this.lotNo,
            forestryBook = this.forestryBook,
            forestryBookNo = this.forestryBookNo,
            invoiceNo = this.invoiceNo,
            fscNo = this.fscNo,
            createdAtFrom = this.uploadedFrom,
            createdAtTo = this.uploadedTo
        )
    }
}
