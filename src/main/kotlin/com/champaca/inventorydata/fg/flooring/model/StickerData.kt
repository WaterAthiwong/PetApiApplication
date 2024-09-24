package com.champaca.inventorydata.fg.flooring.model

import java.math.BigDecimal

data class StickerData(
    val id: Int,
    val batchCode: String,
    val code: String,
    val customer: String,
    val salesOrderNo: String,
    val salesOrderLineNo: String,
    val top: String,
    val type: String,
    val color : String,
    val remark: String,
    val remark2: String,
    val date: String,
    val items: List<StickerItem>,
    val printedAt: String,
    val registeredAt: String,
    val canEdit: Boolean
) {
    data class StickerItem(
        val matCode : String = "",
        val extraAttributes: Map<String, String> ?= emptyMap(),
        val thickness: BigDecimal,
        val width: BigDecimal,
        val height: BigDecimal,
        val qty: BigDecimal,
    )
}
