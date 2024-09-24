package com.champaca.inventorydata.log.request

import com.champaca.inventorydata.log.LogDeliverySearchParams
import com.champaca.inventorydata.log.model.LogDeliveryData

data class UploadLogDeliveryFileParams(
    val poNo: String,
    val deliveryRound: Int,
    val supplier: Int,
    val forestryBook: String,
    val forestryBookNo: String,
    val lotNo: String,
    val fsc: Boolean, // If fsc is set to true, the user needs to provide invoice no and fsc no.
    val invoiceNo: String?,
    val fscNo: String?
) {
    fun toValidateForestryFileParams(): ValidateForestryFileParams {
        return ValidateForestryFileParams(
            fsc = this.fsc,
            supplier = this.supplier,
            scannedRefCodes = null
        )
    }

    fun toLogDeliveryData(): LogDeliveryData {
        return LogDeliveryData(
            supplierId = this.supplier,
            poNo = this.poNo,
            deliveryRound = this.deliveryRound,
            forestryBook = this.forestryBook,
            forestryBookNo = this.forestryBookNo,
            lotNo = this.lotNo,
            fsc = this.fsc,
            invoiceNo = this.invoiceNo,
            fscNo = this.fscNo
        )
    }
}
