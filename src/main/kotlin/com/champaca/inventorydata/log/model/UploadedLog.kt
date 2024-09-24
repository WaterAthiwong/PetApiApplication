package com.champaca.inventorydata.log.model

import com.champaca.inventorydata.common.ChampacaConstant.M3_TO_FT3
import com.champaca.inventorydata.log.LogDeliveryError
import java.math.BigDecimal
import java.time.LocalDateTime

data class UploadedLog(
    val id: Int = -1,
    val logDeliveryId: Int = -1,
    val supplierName: String?,
    val barcode: String?,
    val logNo: String,
    val batchNo: String,
    val poNo: String?,
    val circumference: Int,
    val length: Int,
    val matCode: String,
    val volumnM3: BigDecimal,
    val errorType: LogDeliveryError? = null,
    val receivedAt: LocalDateTime? = null,
    val receivedBy: String? = null,
    val exportedToWmsAt: LocalDateTime? = null,
    val invoiceNo: String? = null,
    val lotNo: String? = null,
) {
    val volumnFt3: BigDecimal
        get() = volumnM3.multiply(M3_TO_FT3)
}
