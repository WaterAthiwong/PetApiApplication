package com.champaca.inventorydata.goodmovement.usecase

import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.goodmovement.GoodMovementError
import com.champaca.inventorydata.goodmovement.request.AddReferenceRequest
import com.champaca.inventorydata.goodmovement.response.ReferenceResponse
import com.champaca.inventorydata.wms.WmsService
import org.springframework.stereotype.Service

@Service
class AddReferenceUseCase(
    val wmsService: WmsService
) {
    fun execute(session: String, request: AddReferenceRequest): ReferenceResponse {
        val result = wmsService.addReferenceGoodMovement(session, request.goodReceiptGoodMovementId, request.pickingOrderGoodMovementIds)
        if (result is ResultOf.Failure) {
            return ReferenceResponse.Failure(
                errorType = GoodMovementError.WMS_VALIDATION_ERROR,
                errorMessage = result.message
            )
        }
        return ReferenceResponse.Success(true)
    }

}