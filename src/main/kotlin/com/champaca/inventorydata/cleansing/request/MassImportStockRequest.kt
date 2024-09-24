package com.champaca.inventorydata.cleansing.request

import java.math.BigDecimal

data class MassImportStockRequest(
    val goodMovementId: Int,
    val departmentPrefix: String,
    val location: String,
    val stocks: List<MassStock>
) {
    data class MassStock(
        val pileCode: String,
        val matCode: String,
        val qty: BigDecimal
    ) {
        lateinit var refCode: String
        var lotNoId: Int = -1
    }
}