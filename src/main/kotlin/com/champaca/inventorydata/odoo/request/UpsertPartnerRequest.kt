package com.champaca.inventorydata.odoo.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpsertPartnerRequest(
    @SerialName("id")
    var odooId: Int = -1,
    val name: String,

    @SerialName("is_customer")
    val isCustomer: Boolean,

    @SerialName("is_supplier")
    val isSupplier: Boolean,
)
