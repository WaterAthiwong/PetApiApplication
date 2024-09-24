package com.champaca.inventorydata.odoo.model.out

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OdooResponse(
    val success: Boolean,
    val message: String,

    @SerialName("status_code")
    val statusCode: Int
)
