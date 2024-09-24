package com.champaca.inventorydata.goodmovement.model

data class GoodMovementData(
    val id: Int,
    val code: String,
    val type: GoodMovementType,
    val processType: String?,
    val processTypeId: Int?,
    val manufacturingLine: String?,
    val manufacturingLineId: Int?,
    val departmentId: Int,
    val department: String,
    val productionDate: String,
    val orderNo: String?,
    val jobNo: String?,
    val poNo: String?,
    val invoiceNo: String?,
    val lotNo: String?,
    val supplierId: Int?,
    val supplier: String?,
    val createdBy: String,
    val approvedBy: String?,
    val remark: String?,
    val extraAttributes: Map<String, String>?,
    val productType: String?,
) {
    var itemCount = 0
    var pileCount = 0
    var totalVolumnFt3 = 0.toBigDecimal()
    var totalVolumnM3 = 0.toBigDecimal()
    var totalAreaM2 = 0.toBigDecimal()
}
