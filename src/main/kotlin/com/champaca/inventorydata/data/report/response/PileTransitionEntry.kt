package com.champaca.inventorydata.data.report.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@JsonIgnoreProperties(value = ["pileId", "skuId", "lotId", "volumnFt3", "extraAttributes", "thickness", "width",
    "length", "lengthUom", "fsc", "grade", "species", "skuGroupId"])
data class PileTransitionEntry(
    val pileCode: String,
    val pileId: Int,
    val matCode: String,
    val skuId: Int,
    val thickness: BigDecimal,
    val width: BigDecimal,
    val length: BigDecimal,
    val lengthUom: String,
    val fsc: Boolean,
    val grade: String,
    val species: String,
    val skuGroupId: Int,
    val lotId: Int,

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val orderNo: String?,

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val jobNo: String?,
    val qty: Int,
    val volumnFt3: BigDecimal,

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val remark: String?,
    val transitionDate: LocalDate,
    val user: String,
    val fromLocation: String,
    val toLocation: String,
    val goodMovementCode: String,
    val extraAttributes: Map<String, String>?,
    val invoiceNo: String?,
    val lotNo: String?,
    val supplierName: String?,
    val destination: String
) {
    val totalVolumnFt3: BigDecimal
        get() = (volumnFt3 * qty.toBigDecimal()).setScale(5, RoundingMode.HALF_UP)

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val sm: String?
        get() = extraAttributes?.get("SM")

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val rc: String?
        get() = extraAttributes?.get("RC")

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val kd: String?
        get() = extraAttributes?.get("KD")

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val rm: String?
        get() = extraAttributes?.get("RM")
}