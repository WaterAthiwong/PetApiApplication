package com.champaca.inventorydata.goodmovement.request

import com.champaca.inventorydata.goodmovement.model.GoodMovementData
import com.champaca.inventorydata.goodmovement.model.GoodMovementType

data class CreateGoodMovementRequest(
    val id: Int = 0,
    val type: String, // This will be either GOODS_RECEIPT or PICKING_ORDER to match with member of GoodMovementType
    val processTypeId: Int?,
    val manufacturingLineId: Int?,
    val departmentId: Int,
    val productionDate: String,
    val orderNo: String?,
    val jobNo: String?,
    val poNo: String?,
    val invoiceNo: String?,
    val lotNo: String?,
    val supplierId: Int?,
    val remark: String?,
    val extraAttributes: Map<String, String>?,
    val productType: String
) {
    fun toGoodMovementData(): GoodMovementData {
        return GoodMovementData(
            id = id,
            code = "",
            type = GoodMovementType.valueOf(type),
            processType = "",
            processTypeId = processTypeId,
            manufacturingLine = "",
            manufacturingLineId = manufacturingLineId,
            departmentId = departmentId,
            department = "",
            productionDate = productionDate,
            orderNo = orderNo,
            jobNo = jobNo,
            poNo = poNo,
            invoiceNo = invoiceNo,
            lotNo = lotNo,
            supplierId = supplierId,
            supplier = "",
            createdBy = "",
            approvedBy = "",
            remark = remark,
            extraAttributes = extraAttributes,
            productType = productType
        )
    }
}
