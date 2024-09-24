package com.champaca.inventorydata.goodmovement.response

import com.champaca.inventorydata.goodmovement.GoodMovementError
import com.champaca.inventorydata.goodmovement.model.GoodMovementData
import com.champaca.inventorydata.goodmovement.model.ReferencedGoodMovement

sealed class GetGoodMovementDataResponse {
    data class Success(
        val goodMovement: GoodMovementData,
        val canApprove: Boolean,
        val canCreateMatchingGoodReceipt: Boolean, // อันนี้สำหรับใบเบิกโดยเฉพาะ ถ้าเป็น true แปลว่ายังไม่มีใบรับสินค้าคู่กัน ถ้าเป็น false แปลว่ามีแล้ว
        val referencedGoodMovements: List<ReferencedGoodMovement>
    ): GetGoodMovementDataResponse()

    data class Failure(
        val errorType: GoodMovementError,
        val errorMessage: String? = null
    ): GetGoodMovementDataResponse()
}
