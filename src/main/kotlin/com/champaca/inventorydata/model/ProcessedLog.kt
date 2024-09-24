package com.champaca.inventorydata.model

import com.champaca.inventorydata.common.ChampacaConstant
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.time.LocalDate

@JsonIgnoreProperties(value = ["skuGroupId", "fsc", "quantity", "species", "extraAttributes", "skuVolumnM3", "skuVolumnFt3"])
class ProcessedLog(
    val goodsMovementCode: String,
    val productionDate: LocalDate,
    val skuGroupId: Int,
    val matCode: String,
    val length: BigDecimal,
    val circumference: BigDecimal,
    val skuVolumnM3: BigDecimal,
    val skuVolumnFt3: BigDecimal,
    val species: String,
    val fsc: Boolean,
    val refCode: String?,
    val extraAttributes: Map<String, String>?,
    private val qty: BigDecimal,
    val manufacturingLine: String?,
    val jobNo: String?,
    val orderNo: String?,
    val poNo: String?,
    val invoiceNo: String?,
    val lotNo: String?
) {
    val volumnM3 = extraAttributes?.get(ChampacaConstant.VOLUMN_M3)?.toBigDecimal() ?: skuVolumnM3

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val logNo: Int
        get() = extraAttributes?.get("logNo")?.toInt() ?: 0

    val quantity: Int
        get() = qty.toInt()

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val forestryBook: String
        get() = extraAttributes?.get("forestryBook") ?: ""

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val forestryBookNo: String
        get() = extraAttributes?.get("forestryBookNo") ?: ""

    val speciesName: String
        get() = Species.valueOf(species)?.longName ?: ""

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val supplierName: String
        get() = extraAttributes?.get("supplier") ?: ""
}