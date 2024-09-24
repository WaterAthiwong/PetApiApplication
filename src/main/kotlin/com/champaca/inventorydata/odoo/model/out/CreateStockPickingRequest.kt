package com.champaca.inventorydata.odoo.model.out

import com.champaca.inventorydata.odoo.model.BigDecimalSerializer
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class CreateStockPickingRequest(
    @SerialName("date_done")
    val dateDone: String,
    val origin: String = "", // Optinal ไม่ใส่ก็ได้ ถ้าใส่ก็น่าจะเป็นแค่ Job No ของ WMS

    @SerialName("location_id")
    val locationId: Int,

    @SerialName("location_dest_id")
    val locationDestinationId: Int,

    @SerialName("picking_type_id")
    val pickingTypeId: Int,

    @SerialName("move_lines")
    val moveLines: List<MoveLine>
) {
    @Serializable
    data class MoveLine(
        @SerialName("product_id")
        val productId: Int,

        @Serializable(with = BigDecimalSerializer::class)
        @SerialName("product_uom_qty")
        val productUomQty: BigDecimal,

        @SerialName("lot_name")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        val lotName: String? = null,

        @SerialName("pack_no")
        val packNo: String,

        @SerialName("wms_ref_no")
        val wmsRefNo: String
    )
}