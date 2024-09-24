package com.champaca.inventorydata.goodmovement.request

import com.champaca.inventorydata.goodmovement.model.GoodMovementType

data class GetGoodMovementRequest(
    val type: GoodMovementType,
    val fromProductionDate: String?,
    val toProductionDate: String?,
    val processTypeId: Int?,
    val manufacturingLineId: Int?,
    val supplierId: Int?,
    val departmentId: Int?,
    val jobNo: String?,
    val orderNo: String?,
    val hasReference: Boolean? = null,
    val purpose: String?
) {
    fun isBlank(): Boolean {
        return processTypeId == null && manufacturingLineId == null &&
            fromProductionDate.isNullOrEmpty() && toProductionDate.isNullOrEmpty() &&
            supplierId == null && departmentId == null && jobNo.isNullOrEmpty() &&
            orderNo.isNullOrEmpty() && hasReference == null && purpose.isNullOrEmpty()
    }
}
