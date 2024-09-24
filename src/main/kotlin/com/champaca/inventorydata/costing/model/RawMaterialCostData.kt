package com.champaca.inventorydata.costing.model

import java.math.BigDecimal

data class RawMaterialCostData(
    val id: Int = 0,
    val supplierId: Int,
    val supplier: String,
    val skuId: Int = 0,
    val matCode: String = "",
    val type: String,
    val poNo: String,
    val deliveryCycle: Int,
    val unitCostM3: BigDecimal = BigDecimal.ZERO,
    val unitCostFt3: BigDecimal = BigDecimal.ZERO,
    val createdAt: String = "",
    val updatedAt: String = "",
) {
    var qty = BigDecimal.ZERO
    var volumnM3 = BigDecimal.ZERO
    var volumnFt3 = BigDecimal.ZERO
    var receivedDate = ""
}