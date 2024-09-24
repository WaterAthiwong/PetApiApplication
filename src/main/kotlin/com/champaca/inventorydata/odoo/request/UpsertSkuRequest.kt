package com.champaca.inventorydata.odoo.request

import com.champaca.inventorydata.odoo.model.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class UpsertSkuRequest(
    @SerialName("id")
    var odooId: Int = -1,
    val name: String,

    @SerialName("display_name")
    val displayName: String,
    val type: String,

    @SerialName("default_code")
    val matCode: String,

    @SerialName("uom_id")
    val uomId: Int,

    @SerialName("uom_name")
    val uomName: String,

    @SerialName("uom_po_id")
    val uomPoId: Int,

    @SerialName("uom_po_name")
    val uomPoName: String,
    val tracking: String,

    @SerialName("cubic_meter_factor")
    @Serializable(with = BigDecimalSerializer::class)
    val volumnM3: BigDecimal,

    @SerialName("square_meter_factor")
    @Serializable(with = BigDecimalSerializer::class)
    val areaM2: BigDecimal,

    @SerialName("cubic_foot_factor")
    @Serializable(with = BigDecimalSerializer::class)
    val volumnFt3: BigDecimal,

    @SerialName("unit_price_per_square_meter")
    @Serializable(with = BigDecimalSerializer::class)
    val pricePerM2: BigDecimal,

    @SerialName("attribute_list")
    val extraAttributes: Map<String, String> = emptyMap()
)