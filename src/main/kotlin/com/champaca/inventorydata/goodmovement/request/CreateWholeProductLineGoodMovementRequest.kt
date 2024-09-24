package com.champaca.inventorydata.goodmovement.request

import com.champaca.inventorydata.goodmovement.model.GoodMovementData
import com.champaca.inventorydata.goodmovement.model.GoodMovementType

data class CreateWholeProductLineGoodMovementRequest(
    val id: Int = 0,
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
    val productType: String,
    val lines: List<ProcessManufacutring>
) {
    data class ProcessManufacutring(
        val processTypeId: Int,
        val manufacturingLineId: Int
    )

    fun toGoodMovementData(): List<GoodMovementData> {
        val pickingGoodMovements = lines.map { line ->
            GoodMovementData(
                id = id,
                code = "",
                type = GoodMovementType.PICKING_ORDER,
                processType = "",
                processTypeId = line.processTypeId,
                manufacturingLine = "",
                manufacturingLineId = line.manufacturingLineId,
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
        val goodReceiptGoodMovements = pickingGoodMovements.map {
            it.copy(type = GoodMovementType.GOODS_RECEIPT)
        }
        return pickingGoodMovements + goodReceiptGoodMovements
    }
}
