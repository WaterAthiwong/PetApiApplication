package com.champaca.inventorydata.log.model
import com.champaca.inventorydata.common.ChampacaConstant.FORESTRY_BOOK
import com.champaca.inventorydata.common.ChampacaConstant.FORESTRY_BOOK_NO
import com.champaca.inventorydata.common.ChampacaConstant.LOG_NO
import com.champaca.inventorydata.common.ChampacaConstant.RECEIVED_DATE
import com.champaca.inventorydata.common.ChampacaConstant.VOLUMN_M3
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.math.RoundingMode

@JsonIgnoreProperties(value = ["lotExtraAttributes", "goodMovementAttributes", "lengthUom", "circumferenceUom", "skuVolumnM3", "skuVolumnFt3"])
class StoredLog(
    val matCode: String,
    val length: BigDecimal,
    val lengthUom: String,
    val circumference: BigDecimal,
    val circumferenceUom: String,
    val grade: String?,
    val species: String,
    val skuVolumnM3: BigDecimal,
    val skuVolumnFt3: BigDecimal,
    val refCode: String?,
    val supplierName: String?,
    val orderNo: String?,
    val invoiceNo: String?,
    val poNo: String?,
    val location: String,
    val storeLocationId: Int,
    val skuId: Int,
    val lotNo: String,
    val lotNoId: Int,
    val goodMovementId: Int,
    val goodMovementCode: String,
    val productionDate: String,
    val lotExtraAttributes: Map<String, String>?,
) {
    val logNo = lotExtraAttributes?.get(LOG_NO) ?: ""
    val volumnM3 = lotExtraAttributes?.get(VOLUMN_M3)?.toBigDecimal() ?: skuVolumnM3
    val forestryBook = lotExtraAttributes?.get(FORESTRY_BOOK) ?: ""
    val forestryBookNo = lotExtraAttributes?.get(FORESTRY_BOOK_NO) ?: ""
    val volumnFt3 = volumnM3.multiply(BigDecimal("35.315")).setScale(2, RoundingMode.HALF_UP)
    val qty = BigDecimal.ONE
    val receivedDate = lotExtraAttributes?.get(RECEIVED_DATE) ?: ""
}