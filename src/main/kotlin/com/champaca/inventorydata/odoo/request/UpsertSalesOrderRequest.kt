package com.champaca.inventorydata.odoo.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpsertSalesOrderRequest(
    @SerialName("id")
    var odooId: Int = -1,
    val name: String,

    @SerialName("partner_id")
    val partnerId: Int,

    @SerialName("warehouse_id")
    val warehouseId: Int,
    val state: String,

    @SerialName("delivery_state")
    val deliveryState: String,

    @SerialName("date_order")
    val dateOrder: String,

    @SerialName("order_line")
    val orderLines: List<OrderLine>
) {
    @Serializable
    data class OrderLine(
        val id: Int,
        val name: String,

        @SerialName("product_id")
        val productId: Int,

        @SerialName("qty_to_deliver")
        val qtyToDeliver: Int,

        @SerialName("ordered_qty")
        val orderedQty: Int,

        @SerialName("ordered_uom")
        val orderedUom: Int,

        @SerialName("delivery_uom")
        val deliveryUom: Int
    )
}