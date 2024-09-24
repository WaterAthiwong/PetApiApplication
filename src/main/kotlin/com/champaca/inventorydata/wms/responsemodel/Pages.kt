package com.champaca.inventorydata.wms.responsemodel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Pages(
    val page: Int,
    @SerialName("total_page")
    val totalPage: Int,
    @SerialName("item_per_page")
    val itemPerPage: Int,
    @SerialName("first_item_number")
    val firstItemNumber: Int,
    @SerialName("last_item_number")
    val lastItemNumber: Int,
    @SerialName("total_item_count")
    val totalItemCount: Int
)