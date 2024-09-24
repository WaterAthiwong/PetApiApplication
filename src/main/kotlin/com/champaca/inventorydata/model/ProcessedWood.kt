package com.champaca.inventorydata.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@JsonIgnoreProperties(value = ["process", "additionalDataStr", "data", "fsc", "species"])
class ProcessedWood(
    val goodsMovementCode: String,
    val productionDate: LocalDate,
    val process: String,
    val manufacturingLine: String,
    val palletCode: String?,
    val matCode: String,
    val width: BigDecimal,
    val widthUom: String,
    val length: BigDecimal,
    val lengthUom: String,
    val thickness: BigDecimal,
    val thicknessUom: String,
    val volumnFt3: BigDecimal,
    val volumnM3: BigDecimal,
    val grade: String?,
    val fsc: Boolean,
    val species: String,
    val jobNo: String?,
    val orderNo: String?,
    val poNo: String?,
    val invoiceNo: String?,
    val quantity: Int,
    val updatedAt: LocalDateTime?,
    val additionalDataStr: String
): AdditionalDataHolder by AdditionalDataHolderImpl(additionalDataStr) {
    var location: String? = null

    val speciesName: String
        get() = Species.valueOf(species)?.longName ?: ""

    val fscCert: String = if (fsc) "FSC" else "NON FSC"

    val totalVolumnFt3: BigDecimal = volumnFt3 * quantity.toBigDecimal()
    val totalVolumnM3: BigDecimal = volumnM3 * quantity.toBigDecimal()
}