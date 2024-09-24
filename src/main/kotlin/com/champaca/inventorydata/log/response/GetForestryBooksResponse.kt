package com.champaca.inventorydata.log.response

data class GetForestryBooksResponse(
    val values: Map<String, List<Entry>>
) {

    data class Entry(
        val forestryBookNo: String,
        val logDeliveryId: Int
    )
}