package com.champaca.inventorydata.conditionroom.model

import java.math.BigDecimal

data class SuggestedShelf(
    val shelfCode: String,
    val location: String,
    val matCode: String,
    val qty: BigDecimal,
    val thickness: BigDecimal,
    val width: BigDecimal,
    val length: BigDecimal,
    val grade: String
)