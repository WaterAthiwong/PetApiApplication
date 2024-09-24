package com.champaca.inventorydata.fg.flooring.request

data class GetBatchesRequest(
    val createdDateFrom: String?,
    val createdDateTo: String?,
    val salesOrderPattern: String?,
    val customerId: Int?
)