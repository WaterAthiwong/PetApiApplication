package com.champaca.inventorydata.log.request

data class CreateLogsInWmsRequest(
    val logIds: List<Int>,
    val jobNo: String,
    val location: String
)