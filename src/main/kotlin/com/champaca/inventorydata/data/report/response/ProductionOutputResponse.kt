package com.champaca.inventorydata.data.report.response

import java.math.BigDecimal

data class ProductionOutputResponse(
    val transactions: List<PileTransactionEntry>,
    val totalPiles: Int,
    val totalPieces: BigDecimal,
    val totalAreaM2: BigDecimal,
)