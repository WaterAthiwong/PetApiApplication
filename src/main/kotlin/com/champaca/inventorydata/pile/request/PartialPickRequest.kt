package com.champaca.inventorydata.pile.request

import java.math.BigDecimal

data class PartialPickRequest(
    val pileCode: String,
    val pileId: Int,
    val fromGoodMovementId: Int,
    val toGoodMovementId: Int,
    val manufacturingLineId: Int?,
    val pickedItems: List<Item>
) {
    data class Item(
        val skuId: Int,
        val lotNoId: Int,
        val qty: BigDecimal
    )
}

