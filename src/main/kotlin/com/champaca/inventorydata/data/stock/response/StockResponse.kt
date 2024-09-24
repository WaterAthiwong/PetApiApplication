package com.champaca.inventorydata.data.stock.response

import com.champaca.inventorydata.model.ItemStock
import java.math.BigDecimal

data class StockResponse(
    val stocks: List<ItemStock>,
    val totalPiles: Int,
    val totalPieces: BigDecimal,
    val totalVolumnFt3: BigDecimal,
)