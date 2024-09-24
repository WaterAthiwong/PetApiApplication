package com.champaca.inventorydata.log.model

import java.time.LocalDateTime

data class ReceiveLogStatus(
    val poNo: String,
    val deliveryRound: Int,
    val totalLogs: Int,
    val alreadyReceived: Int,
    val notYetReceived: Int,
    val createAt: LocalDateTime
)