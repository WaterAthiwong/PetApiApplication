package com.champaca.inventorydata.wms.responsemodel

data class UserAuthJsonResponse(
    val header: Header,
    val data: Map<String, String>?
)