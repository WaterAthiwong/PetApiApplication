package com.champaca.inventorydata.masterdata.sku.model

import java.math.BigDecimal

data class SkuData(
    val skuGroupId: Int,
    val skuGroupName: String,
    val code: String,
    val matCode: String,
    val name: String,
    val thickness: BigDecimal,
    val width: BigDecimal,
    val length: BigDecimal,
    val widthUom: String,
    val lengthUom: String,
    val thicknessUom: String,
    val circumference: BigDecimal,
    val circumferenceUom: String,
    val volumnM3: BigDecimal,
    val volumnFt3: BigDecimal,
    val areaM2: BigDecimal,
    val species: String,
    val grade: String?,
    val fsc: String,
    val extraAttributes: Map<String, String> = mapOf()
) {
}