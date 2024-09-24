package com.champaca.inventorydata.pile.request

import java.math.BigDecimal

data class PileItem(
    val skuId: Int,
    val matCode: String,
    val qty: BigDecimal,
    val lotRefCode: String?,
) {
    var lotNoId = -1
}