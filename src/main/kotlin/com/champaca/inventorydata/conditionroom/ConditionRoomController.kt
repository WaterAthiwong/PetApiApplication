package com.champaca.inventorydata.conditionroom

import com.champaca.inventorydata.common.ChampacaConstant
import com.champaca.inventorydata.conditionroom.request.TransferToStorageRequest
import com.champaca.inventorydata.conditionroom.request.AssembleRequest
import com.champaca.inventorydata.conditionroom.request.TransferToProductionRequest
import com.champaca.inventorydata.conditionroom.response.*
import com.champaca.inventorydata.pile.request.PickPileRequest
import com.champaca.inventorydata.conditionroom.usecase.*
import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.response.PickPileResponse
import com.champaca.inventorydata.wms.WmsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RequestMapping("/conditionRoom")
@RestController
@CrossOrigin(origins = ["*"])
class ConditionRoomController(
    val accumulatePileUseCase: AccumulatePileUseCase,
    val transferToStorageUseCase: TransferToStorageUseCase,
    val transferToProductionUseCase: TransferToProductionUseCase,
    val assembleUseCase: AssembleUseCase,
    val getRemainingItemsUseCase: GetRemainingItemsUseCase,
    val getStuckInTransferItemsUseCase: GetStuckInTransferItemsUseCase
) {
    companion object {
        const val INCOMING_PICK_PREFIX = "IncomingPickingOrder."
        const val INCOMING_RECEIVE_PREFIX = "IncomingGoodReceipt."
        const val OUTGIONG_PICK_PREFIX = "OutgoingPickingOrder."
        const val OUTGIONG_RECEIVE_PREFIX = "OutgoingGoodReceipt."
        const val CONDITION_ROOM_INCOMING_PICK_PREFIX = INCOMING_PICK_PREFIX + "CO."
        const val CONDITION_ROOM_INCOMING_RECEIVE_PREFIX = INCOMING_RECEIVE_PREFIX + "CO."
        const val CONDITION_ROOM_OUTGOING_PICK_PREFIX = OUTGIONG_PICK_PREFIX + "CO."
        const val CONDITION_ROOM_OUTGOING_RECEIVE_PREFIX = OUTGIONG_RECEIVE_PREFIX + "CO."
    }

    @PostMapping("/wms/accumulate")
    fun accumulatePile(@RequestAttribute(WmsService.WMS_SESSION) sessionId: String,
                       @RequestAttribute(ChampacaConstant.USER_ID) userId: String,
                       @RequestBody request: PickPileRequest): ResponseEntity<Any> {
        val result = accumulatePileUseCase.execute(sessionId, userId, request)
        return if (result is PickPileResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/transfer/storage")
    fun transferToStorage(@RequestAttribute(WmsService.WMS_SESSION) sessionId: String,
                          @RequestAttribute(ChampacaConstant.USER_ID) userId: String,
                          @RequestBody request: TransferToStorageRequest): ResponseEntity<Any> {
        val result = transferToStorageUseCase.execute(sessionId, userId, request)
        return if (result is TransferToStorageResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/transfer/production")
    fun transferToProduction(@RequestAttribute(WmsService.WMS_SESSION) sessionId: String,
                             @RequestAttribute(ChampacaConstant.USER_ID) userId: String,
                             @RequestBody request: TransferToProductionRequest): ResponseEntity<Any> {
        val result = transferToProductionUseCase.execute(sessionId, userId, request)
        return if (result is TransferToProductionResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/assemble")
    fun assemble(@RequestAttribute(WmsService.WMS_SESSION) sessionId: String,
                 @RequestAttribute(ChampacaConstant.USER_ID) userId: String,
                 @RequestBody request: AssembleRequest
    ): ResponseEntity<Any> {
        val result = assembleUseCase.execute(sessionId, userId, request)
        return if (result is AssembleResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @GetMapping("/remainingItems/{goodMovementId}")
    fun getRemainingItems(@PathVariable goodMovementId: Int): ResponseEntity<Any> {
        val result = getRemainingItemsUseCase.execute(goodMovementId)
        return if (result is GetRemainingItemsResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @GetMapping("/remainingItems/stuck")
    fun getStuckInTransferItems(): List<MovingItem> {
        return getStuckInTransferItemsUseCase.execute()
    }
}