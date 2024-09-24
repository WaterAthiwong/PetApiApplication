package com.champaca.inventorydata.fg.flooring.model

data class BatchData(
    val id: Int,
    val code: String,
    var qty: Int = 0,
    var registeredQty: Int = 0,
    val salesOrderNo: String,
    val salesOrderLineNo: String,
    var matCode: String = "",
    val format: String,
    val customer: String,
    val createdAt: String,
    val remark: String? = "",
    val remark2: String? = "",
)