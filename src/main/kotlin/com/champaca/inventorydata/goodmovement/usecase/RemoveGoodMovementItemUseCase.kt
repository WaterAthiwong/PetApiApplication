package com.champaca.inventorydata.goodmovement.usecase

import com.champaca.inventorydata.goodmovement.request.RemoveGoodMovementItemRequest
import com.champaca.inventorydata.wms.WmsService
import org.springframework.stereotype.Service

@Service
class RemoveGoodMovementItemUseCase(
    val wmsService: WmsService
) {
    fun execute(session: String, userId: String, request: RemoveGoodMovementItemRequest) {
        wmsService.removeGmItem(session, request.goodMovememntItemIds)
    }
}