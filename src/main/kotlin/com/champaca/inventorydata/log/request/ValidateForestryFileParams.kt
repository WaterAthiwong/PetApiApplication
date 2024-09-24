package com.champaca.inventorydata.log.request

data class ValidateForestryFileParams(
    val fsc: Boolean,
    val scannedRefCodes: List<String>?,
    val supplier: Int
)