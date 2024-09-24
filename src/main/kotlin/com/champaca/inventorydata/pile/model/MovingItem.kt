package com.champaca.inventorydata.pile.model

import com.champaca.inventorydata.model.SkuDetail
import com.champaca.inventorydata.model.SkuDetailImpl
import java.math.BigDecimal

data class MovingItem(
    val lotNoId: Int,
    val lotCode: String,
    val lotRefCode: String,
    override var skuId: Int,
    override var matCode: String,
    override var skuName: String,
    override var width: BigDecimal,
    override var widthUom: String,
    override var length: BigDecimal,
    override var lengthUom: String,
    override var thickness: BigDecimal,
    override var thicknessUom: String,
    override var volumnFt3: BigDecimal,
    override var volumnM3: BigDecimal,
    override var areaM2: BigDecimal = BigDecimal.ZERO,
    override var grade: String?,
    override var fsc: Boolean,
    override var species: String,
    val skuGroupId: Int,
    val storeLocationId: Int,
    val storeLocationCode: String,
    var qty: BigDecimal
): SkuDetail by SkuDetailImpl(
    skuId = skuId,
    matCode = matCode,
    skuName = skuName,
    width = width,
    widthUom = widthUom,
    length = length,
    lengthUom = lengthUom,
    thickness = thickness,
    thicknessUom = thicknessUom,
    circumference = (-1.0).toBigDecimal(),
    circumferenceUom = "",
    volumnFt3 = volumnFt3,
    volumnM3 = volumnM3,
    areaM2 = areaM2,
    grade = grade,
    fsc = fsc,
    species = species
) {
    var pilecode: String = ""
    var erpGroupCode: String = ""
    var skuGroupName: String = ""
    var skuMainGroupName: String = ""
    var gmItemId = -1
    fun clone() = copy()
}

fun List<MovingItem>.reduceBySkuId(): List<MovingItem> {
    return groupBy { it.skuId }.mapValues { (skuId, items) ->
        val first = items.first()
        first.copy(qty = items.sumOf { it.qty })
    }
        .values.toList()
}