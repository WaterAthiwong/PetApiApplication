package com.champaca.inventorydata.log.model

import com.champaca.inventorydata.log.LogDeliveryError
import java.math.BigDecimal
import java.time.LocalDateTime

data class LogData(
    val logDeliveryId: Int,
    var receivingUserId: Int = -1,
    var exportingUserId: Int = -1,
    var goodsMovementId: Int = -1,
    val batchNo: String,
    val itemOrder: Int = -1,
    val species: String,
    val length: Int,
    val circumference: Int,
    val volumnM3: BigDecimal,
    val logNo: String,
    val refCode: String,
    val matCode: String,
    var errorType: LogDeliveryError?,
    var receivedAt: LocalDateTime? = null,
    var exportedToWmsAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    var updatedAt: LocalDateTime,
    val storeLocationId: Int = -1,
    val storeLocation: String = ""
) {
    var id: Int = -1
}