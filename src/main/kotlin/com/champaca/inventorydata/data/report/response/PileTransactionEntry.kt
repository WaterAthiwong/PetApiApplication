package com.champaca.inventorydata.data.report.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@JsonIgnoreProperties(value = ["pileId", "skuId", "lotId", "volumnFt3", "areaM2","lengthUom", "skuGroupId",
    "manufacturingLineId", "transactionAt", "goodMovementExtraAttributes", "pileExtraAttributes"])
data class PileTransactionEntry(
    var type: String,
    var pileCode: String,
    var pileId: Int,
    val matCode: String,
    val skuId: Int,
    val skuName: String,
    val thickness: BigDecimal,
    val width: BigDecimal,
    val length: BigDecimal,
    val lengthUom: String,
    val fsc: Boolean,
    val grade: String,
    val species: String,
    val skuGroupId: Int,
    val lotId: Int,
    val orderNo: String?,
    val jobNo: String?,
    val qty: BigDecimal,
    val volumnFt3: BigDecimal,
    val areaM2: BigDecimal,
    val remark: String?,
    val date: LocalDate,
    val goodMovementCode: String,
    val manufacturingLineId: Int?,
    val manufacturingLine: String?,
    val supplier: String?,
    val department: String,
    val lotRefCode: String,
    val productionDate: LocalDate,
    val poNo: String?,
    val lotNo: String?,
    val invoiceNo: String?,
    val goodMovementOrderNo: String?,
    val goodMovementExtraAttributes: Map<String, String>?,
    val pileExtraAttributes: Map<String, String>?,
    val transactionAt: LocalDateTime,
    val pileType: String
) {
    val totalVolumnFt3: BigDecimal
        get() = (volumnFt3 * qty).setScale(5, RoundingMode.HALF_UP)

    val totalAreaM2: BigDecimal
        get() = (areaM2 * qty).setScale(5, RoundingMode.HALF_UP)

    val customer: String
        get() = pileExtraAttributes?.get("customer") ?: goodMovementExtraAttributes?.get("customer") ?: ""

    var transtionAtStr: String = ""
}