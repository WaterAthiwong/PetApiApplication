package com.champaca.inventorydata.masterdata.supplier.model

import com.fasterxml.jackson.annotation.JsonProperty

data class SupplierData(
    val id: Int = -1,
    val type: String,
    @JsonProperty("label") val name: String,
    val taxNo: String?,
    val address: String?,
    val phone: String?,
    val email: String?,
    val contact: String?,
    val erpCode: String?,
    val remark: String?
)