package com.champaca.inventorydata.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.math.RoundingMode

@JsonIgnoreProperties(value = ["volumnFt3", "areaM2", "fsc", "extra"])
data class ItemStock(
    val jobNo: String?,
    val pileId: Int,
    val pileCode: String?,
    val matCode: String,
    val qty: BigDecimal,
    val volumnFt3: BigDecimal,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val location: String? = null,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val manufacturingLine: String? = null,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val supplier: String?,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val orderNo: String?,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val poNo: String?,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val invoiceNo: String?,
    val productionDate: String,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val remark: String?,
    val extra: Map<String, String>,
    val thickness: BigDecimal,
    val thicknessUom: String,
    val width: BigDecimal,
    val widthUom: String,
    val grade: String?,
    val length: BigDecimal,
    val lengthUom: String,
    val areaM2: BigDecimal,
    val mainGroupName: String,
    val groupName: String,
    val species: String
) {
    val totalVolumnFt3: BigDecimal
        get() = volumnFt3.multiply(qty).setScale(2, RoundingMode.HALF_UP)

    val totalAreaM2: BigDecimal
        get() = areaM2.multiply(qty).setScale(2, RoundingMode.HALF_UP)

    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val sm: String?
        get() = extra["SM"]

    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val rc: String?
        get() = extra["RC"]

    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val kd: String?
        get() = extra["KD"]

    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val rm: String?
        get() = extra["RM"]

    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val customer: String?
        get() = extra["customer"]

    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val thicknessMm: BigDecimal
        get() = convertToMm(thickness, thicknessUom)

    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val widthMm: BigDecimal
        get() = convertToMm(width, widthUom)

    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val lengthMm: BigDecimal
        get() = convertToMm(length, lengthUom)

    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    var relocationDate: String? = null

    private fun convertToMm(value: BigDecimal, uom: String): BigDecimal {
        return when(uom) {
            "in" -> value.multiply(25.4.toBigDecimal())
            "mm" -> value
            "m" -> value.multiply(1000.0.toBigDecimal())
            "cm" -> value.multiply(10.0.toBigDecimal())
            "ft" -> value.multiply(304.8.toBigDecimal())
            else -> BigDecimal.ZERO
        }
    }
}