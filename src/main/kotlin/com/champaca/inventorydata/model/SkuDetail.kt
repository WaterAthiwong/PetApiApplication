package com.champaca.inventorydata.model

import java.math.BigDecimal

interface SkuDetail {
    var skuId: Int
    var code: String
    var matCode: String
    var skuName: String
    var width: BigDecimal
    var widthUom: String
    var length: BigDecimal
    var lengthUom: String
    var thickness: BigDecimal
    var thicknessUom: String
    var circumference: BigDecimal?
    var circumferenceUom: String?
    var volumnFt3: BigDecimal
    var volumnM3: BigDecimal
    var areaM2: BigDecimal
    var grade: String?
    var fsc: Boolean
    var species: String
}

class SkuDetailImpl(
    override var skuId: Int = -1,
    override var matCode: String,
    override var skuName: String,
    override var width: BigDecimal,
    override var widthUom: String,
    override var length: BigDecimal,
    override var lengthUom: String,
    override var thickness: BigDecimal,
    override var thicknessUom: String,
    override var circumference: BigDecimal?,
    override var circumferenceUom: String?,
    override var volumnFt3: BigDecimal,
    override var volumnM3: BigDecimal,
    override var areaM2: BigDecimal,
    override var grade: String?,
    override var fsc: Boolean,
    override var species: String
): SkuDetail {

    private var pCode = ""

    override var code: String
        get() = pCode
        set(value) { pCode = value }
}