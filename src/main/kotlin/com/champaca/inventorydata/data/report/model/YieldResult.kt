package com.champaca.inventorydata.data.report.model

import java.math.BigDecimal

data class YieldResult(
    val jobNo: String,
    val lotNo: String?,
    val manufacturingLine: String,
    val supplier: String?,
    val incomings: Int,
    val incomingVolumnM3: BigDecimal,
    val incomingVolumnFt3: BigDecimal,
    var outgoings: Int,
    var outgoingVolumnFt3: BigDecimal,
    var yield: BigDecimal,
    val status: String?,
    val type: String,
    val extraAttributes: Map<String, String> = emptyMap()
)