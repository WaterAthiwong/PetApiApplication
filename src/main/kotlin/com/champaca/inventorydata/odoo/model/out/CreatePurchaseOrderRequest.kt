package com.champaca.inventorydata.odoo.model.out

import com.champaca.inventorydata.odoo.model.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class CreatePurchaseOrderRequest(
    @SerialName("partner_id")
    val partnerId: Int, // สวนป่าที่ส่งไม้ให้

    @SerialName("vendor_reference")
    val vendorReference: String = "", // เลข PO ที่น้ำได้จากหนุ่ม ใน WMS อยู่ในช่อง PO

    @SerialName("order_type")
    val orderType: Int,

    @SerialName("order_line")
    val orderLines: List<OrderLine>,

    @SerialName("contract_no")
    val contractNo: String = "", // เดี๋ยวอันนี้จะสร้าง field contract no

    @SerialName("pr_no")
    val prNo: String
) {
    @Serializable
    data class OrderLine(
        @SerialName("product_id")
        val productId: Int,  // product id ของ Item ใน Odoo

        val name: String, // Optional ชื่อของ Item

        @Serializable(with = BigDecimalSerializer::class)
        val quantity: BigDecimal, // ถ้าเป็นไม้ซุง ส่งเป็นลบ.ม.

        @Serializable(with = BigDecimalSerializer::class)
        @SerialName("unit_price")
        val unitPrice: BigDecimal,

        @SerialName("lot_line")
        val lotLines: List<LotLine> // กรณีที่ไม่มี Lot อันนี้ส่งเป็น empty list ไปเลย
    )

    @Serializable
    data class LotLine(
        @SerialName("lot_name")
        val lotName: String = "",

        @Serializable(with = BigDecimalSerializer::class)
        val quantity: BigDecimal,

        @SerialName("no_trencher")
        val noTrencher: String = "",

        @SerialName("book_number")
        val bookNumber: String = "",

        @SerialName("serial_number")
        val serialNumber: String = "",

        val forest: String = ""
    )
}