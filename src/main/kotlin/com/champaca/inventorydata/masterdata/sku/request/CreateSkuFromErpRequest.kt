package com.champaca.inventorydata.masterdata.sku.request

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class CreateSkuFromErpRequest(
    val rowOrder: String? = null, // ERP: roworder
    val matCode: String? = null, // ERP: itemcode
    val name: String? = null, // ERP: itemname
    val productGroup: String? = null, // ERP: prodgrp e.g. R2, R9, R0, etc.
    val thickness: Double = 0.0, // ERP: thick
    val width: Double = 0.0, // ERP: wide
    val length: Double = 0.0, // ERP: length
    val thicknessUom: String? = null,
    val widthUom: String? = null,
    val lengthUom: String? = null,
    val volumnFt3: Double? = null,
    val species: String? = null, // ERP: woodtype
    val grade: String? = null, // ERP: grade
    val fsc: String? = null // ERP: certifcation
)