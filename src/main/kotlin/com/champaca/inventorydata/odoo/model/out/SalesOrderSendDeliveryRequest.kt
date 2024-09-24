package com.champaca.inventorydata.odoo.model.out

import com.champaca.inventorydata.odoo.model.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class SalesOrderSendDeliveryRequest(
    @SerialName("date_done")
    val dateDone: String,

    @SerialName("location_id")
    val locationId: Int,

    @SerialName("order_line")
    val orderLines: List<OrderLine>
) {
    @Serializable
    data class OrderLine(
        @SerialName("sale_line_id")
        val salesLineId: Int,

        @SerialName("product_id")
        val productId: Int,

        @SerialName("pack_no")
        val packNo: String,

        @Serializable(with = BigDecimalSerializer::class)
        @SerialName("done_qty")
        val doneQty: BigDecimal
    )
}
