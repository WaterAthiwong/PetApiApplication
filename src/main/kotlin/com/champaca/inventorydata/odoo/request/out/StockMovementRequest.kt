package com.champaca.inventorydata.odoo.request.out

data class StockMovementRequest(
    val movedDate: String,
    val departmentId: Int
)