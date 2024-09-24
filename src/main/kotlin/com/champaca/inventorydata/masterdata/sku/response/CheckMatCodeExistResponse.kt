package com.champaca.inventorydata.masterdata.sku.response

data class CheckMatCodeExistResponse(
    val isValid: Boolean,
    val matCode: String? = null,
    val skuId: Int? = null
)