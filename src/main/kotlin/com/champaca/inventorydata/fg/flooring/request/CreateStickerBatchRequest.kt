package com.champaca.inventorydata.fg.flooring.request

import com.champaca.inventorydata.pile.request.PileItem

data class CreateStickerBatchRequest(
    val departmentPrefix: String,
    val customerId: Int,
    val salesOrderNo: String,
    val salesOrderLineNo: String,
    val format: String,
    val productionDate: String,
    val remark: String?,
    val remark2: String?,
    val items: List<PileItem> = emptyList(),
    val copies: Int = 0,
    val fragmentQtys: List<Int> = emptyList()
)