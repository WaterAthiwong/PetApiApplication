package com.champaca.inventorydata.goodmovement.usecase

import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.goodmovement.GoodMovementError
import com.champaca.inventorydata.goodmovement.response.CreateGoodMovementResponse
import com.champaca.inventorydata.goodmovement.response.RemoveGoodMovementResponse
import com.champaca.inventorydata.wms.WmsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RemoveGoodMovementUseCase(
    val wmsService: WmsService
) {
    val logger = LoggerFactory.getLogger(RemoveGoodMovementUseCase::class.java)
    fun execute(session: String, goodMovementId: Int): RemoveGoodMovementResponse {
        val result = wmsService.removeGoodMovement(session, goodMovementId)
        if (result is ResultOf.Failure) {
            logger.warn("Failed to remove good movement ${goodMovementId}: ${result.message}")
            return RemoveGoodMovementResponse.Failure(
                errorType = GoodMovementError.WMS_VALIDATION_ERROR,
                errorMessage = result.message
            )
        } else {
            return RemoveGoodMovementResponse.Success(goodMovementId)
        }
    }
}