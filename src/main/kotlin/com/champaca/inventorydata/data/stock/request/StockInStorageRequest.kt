package com.champaca.inventorydata.data.stock.request

import java.math.BigDecimal

data class StockInStorageRequest(
    val species: String?,
    val thickness: BigDecimal?,
    val width: BigDecimal?,
    val length: BigDecimal?,
    val grade: String?,
    val locationPattern: String?,
    val excludedLocationPattern: String?,
    val locations: List<String>?,
    val itemOnly: Boolean = false,
    val relocateFromDepartmentIds: List<Int> = emptyList(),
)