package com.champaca.inventorydata.wms.responsemodel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ManufactuingLineIndexResponse(
    val header: Header,
    val data: Data
) {
    @Serializable
    data class Data(
        val results: List<Result>,
        val pages: Pages
    )

    @Serializable
    data class Result(
        val id: String,
        @SerialName("process_type_id")
        val processTypeId: String,
        val name: String,
        val status: String,
        @SerialName("process_type")
        val processType: String,
        val prefix: String,
        val qty: String? // Nullable because "qty" can be null
    )
}