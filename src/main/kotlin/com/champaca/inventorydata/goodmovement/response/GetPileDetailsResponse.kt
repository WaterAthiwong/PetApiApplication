package com.champaca.inventorydata.goodmovement.response

import java.math.BigDecimal
import java.math.RoundingMode

data class GetPileDetailsResponse(
    val piles: List<PileData>,
    val pileCount: Int,
    val itemCount: BigDecimal,
    val totalVolumnFt3: BigDecimal
) {
    data class PileData(
        val id: Int,
        val code: String,
        val productGroup: String,
        val orderNo: String?,
        val remark: String?,
        val canUndo: Boolean,
        val canEdit: Boolean,
        val canRemove: Boolean,
        val lotSet: Int,
        val totalQty: BigDecimal,
        val totalVolumnFt3: BigDecimal,
        val totalAreaM2: BigDecimal?,
        val location: String,
        val isPartialPick: Boolean,
        val items: List<Item>,
    )

    data class Item(
        val matCode: String,
        val qty: BigDecimal,
        val volumnFt3: BigDecimal,
        val areaM2: BigDecimal?,
        val recordedAt: String,
        val location: String,
        val gmItemId: Int,
    )
}