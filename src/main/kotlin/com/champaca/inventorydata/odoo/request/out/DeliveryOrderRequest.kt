package com.champaca.inventorydata.odoo.request.out

data class DeliveryOrderRequest(
    val deliveredDate: String,
    val salesOrderNos: List<String> = emptyList()
)
