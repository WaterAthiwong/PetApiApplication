package com.champaca.inventorydata.goodmovement.response

import com.champaca.inventorydata.goodmovement.model.GoodMovementData
import java.math.BigDecimal

data class GetLogDetailsResponse(
    val logs: List<Log>,
    val itemCount: Int,
    val totalVolumnM3: BigDecimal,
    val unPickedLogs: List<UnpickedLog> = emptyList(),
    val unPickedLogItemCount: Int = 0,
    val unPIckedLogTotalVolumnM3: BigDecimal = BigDecimal.ZERO
) {
    data class Log(
        val matCode: String,
        val qty: Int,
        val volumnM3: BigDecimal,
        val areaM2: BigDecimal?,
        val recordedAt: String,
        val barcode: String,
        val logNo: String,
        val location: String,
        val lotNoId: Int,
        val isBooked: Boolean,
        var supplier: String,
        val forestryBook: String,
        val forestryBookNo: String
    )

    data class UnpickedLog(
        val matCode: String,
        val volumnM3: BigDecimal,
        val areaM2: BigDecimal?,
        val barcode: String,
        val logNo: String,
        val location: String,
        val lotNoId: Int,
        val supplier: String,
        val forestryBook: String,
        val forestryBookNo: String
    )
}