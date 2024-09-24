package com.champaca.inventorydata.data.report.response

import java.math.BigDecimal

data class GetKilnStatusResponse(
    val statuses: List<KilnStatus>
) {
    data class KilnStatus(
        val kiln: String,
        val description: String = "",
        val thicknesses: String = "",
        val maxCapacity: BigDecimal? = null,
        val inputFt3: BigDecimal? = null,
        val utilise: String = "",
        val startDryingDate: String? = "",
        val expectedDate: String? = "",
        val days: Int? = null,
        val humidityIn: String? = "",
        val humidityActual: String = "",
        val status: String,
        val orders: String = "",
        val qty: BigDecimal? = null,
        val averageThickness: BigDecimal? = null, // หน่วยเป็นนิ้ว
        val jobNo: String? = "",
        val goodMovementId: Int = -1
    )
}