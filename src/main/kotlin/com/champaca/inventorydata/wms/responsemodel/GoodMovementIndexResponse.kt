package com.champaca.inventorydata.wms.responsemodel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoodMovementIndexResponse(
    val header: Header,
    val data: Data
) {
    @Serializable
    data class Data(
        val results: List<Result>,
        val pages: Pages
    )

    @Serializable
    data class Result(
        val id: String,

        @SerialName("good_movement_id")
        val goodMovementId: String?,

        @SerialName("good_movement_transfer_id")
        val goodMovementTransferId: String?,

        @SerialName("manufacturing_line_id")
        val manufacturingLineId: String,

        @SerialName("supplier_id")
        val supplierId: String?,

        @SerialName("user_id")
        val userId: String,
        val code: String,
        val type: String,

        @SerialName("created_at")
        val createdAt: String,

        @SerialName("order_no")
        val orderNo: String?,

        @SerialName("job_no")
        val jobNo: String?,

        @SerialName("po_no")
        val poNo: String?,

        @SerialName("invoice_no")
        val invoiceNo: String?,

        @SerialName("lot_no")
        val lotNo: String?,

        @SerialName("production_date")
        val productionDate: String,
        val remark: String?,

        @SerialName("approve_user_id")
        val approveUserId: String?,

        @SerialName("close_user_id")
        val closeUserId: String?,

        @SerialName("approved_at")
        val approvedAt: String?,

        @SerialName("closed_at")
        val closedAt: String?,

        @SerialName("is_transfer")
        val isTransfer: String?,

        @SerialName("extra_attributes")
        val extraAttributes: String?,
        val status: String,

        @SerialName("refer_good_movement")
        val referGoodMovement: String?,

        @SerialName("good_movement_transfer")
        val goodMovementTransfer: String?,

        @SerialName("manufacturing_line")
        val manufacturingLine: String?,

        @SerialName("process_type_id")
        val processTypeId: String?,

        @SerialName("process_type")
        val processType: String?,
        val supplier: String?,
        val user: String,
        val approver: String?,

        @SerialName("closed_by")
        val closedBy: String?
    )
}