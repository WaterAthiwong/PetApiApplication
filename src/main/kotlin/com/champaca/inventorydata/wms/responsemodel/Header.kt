package com.champaca.inventorydata.wms.responsemodel

import kotlinx.serialization.Serializable

@Serializable
data class Header(
    val status: String,
    val code: Int,
    val message: String
)