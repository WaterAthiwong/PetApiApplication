package com.champaca.inventorydata.data.stock.request

data class StockInProcessRequest(
    val processId: Int,
    val manufacturingLineIds: List<Int>?
)