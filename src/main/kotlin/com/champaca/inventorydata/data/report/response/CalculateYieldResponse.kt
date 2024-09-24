package com.champaca.inventorydata.data.report.response

import com.champaca.inventorydata.data.report.model.YieldResult
import java.math.BigDecimal

data class CalculateYieldResponse(
    val entries: List<YieldResult>,
    val totalIncomings: Int,
    val totalIncomingVolumnFt3: BigDecimal,
    val totalOutgoings: Int,
    val totalOutgoingVolumnFt3: BigDecimal,
    val totalYield: BigDecimal
)