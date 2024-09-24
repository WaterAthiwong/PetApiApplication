package com.champaca.inventorydata.goodmovement.response

import kotlinx.serialization.Serializable

data class GetExtraAttributesResponse(
    val extraAttributes: List<ExtraAttributes>
) {
    @Serializable
    data class ExtraAttributes(
        val name: String,
        val label: String,
        val type: String,
    )
}