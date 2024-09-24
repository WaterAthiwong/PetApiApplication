package com.champaca.inventorydata.log.model

data class LogDeliveryData(
    val supplierId: Int,
    val poNo: String,
    val deliveryRound: Int,
    val forestryBook: String,
    val forestryBookNo: String,
    val lotNo: String?,
    val fsc: Boolean,
    val invoiceNo: String?,
    val fscNo: String?
) {
    var id: Int = -1
    lateinit var supplierName: String
}
