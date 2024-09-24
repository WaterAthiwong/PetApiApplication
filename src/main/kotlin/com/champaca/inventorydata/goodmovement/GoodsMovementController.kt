package com.champaca.inventorydata.goodmovement

import com.champaca.inventorydata.common.ChampacaConstant.USER_ID
import com.champaca.inventorydata.goodmovement.model.GoodMovementData
import com.champaca.inventorydata.goodmovement.request.*
import com.champaca.inventorydata.goodmovement.response.*
import com.champaca.inventorydata.goodmovement.usecase.*
import com.champaca.inventorydata.wms.WmsService.Companion.WMS_SESSION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RequestMapping("/goodsMovement")
@RestController
@CrossOrigin(origins = ["*"])
class GoodsMovementController(
    val getGoodMovementUseCase: GetGoodMovementUseCase,
    val createGoodMovementUseCase: CreateGoodMovementUseCase,
    val editGoodMovementUseCase: EditGoodMovementUseCase,
    val removeGoodMovementUseCase: RemoveGoodMovementUseCase,
    val approveGoodMovementUseCase: ApproveGoodMovementUseCase,
    val getGoodMovementDataUseCase: GetGoodMovementDataUseCase,
    val getPileDetailsUseCase: GetPileDetailsUseCase,
    val getLogDetailsUseCase: GetLogDetailsUseCase,
    val getExtraAttributesUseCase: GetExtraAttributesUseCase,
    val addReferenceUseCase: AddReferenceUseCase,
    val removeReferenceUseCase: RemoveReferenceUseCase,
    val createMatchingGoodReceiptUseCase: CreateMatchingGoodReceiptUseCase,
    val removeGoodMovementItemUseCase: RemoveGoodMovementItemUseCase,
    val createWholeProductLineGoodMovementUseCase: CreateWholeProductLineGoodMovementUseCase
) {

    @PostMapping("/wms/")
    fun getGoodsMovements(@RequestAttribute(WMS_SESSION) sessionId: String, @RequestBody request: GetGoodMovementRequest): List<GoodMovementData> {
        return getGoodMovementUseCase.execute(sessionId, request)
    }

    @PostMapping("/wms/create")
    fun createGoodMovement(@RequestAttribute(WMS_SESSION) sessionId: String,
                           @RequestAttribute(USER_ID) userId: String,
                           @RequestBody request: CreateGoodMovementRequest): ResponseEntity<Any> {
        val result = createGoodMovementUseCase.execute(sessionId, userId, request)
        if (result is CreateGoodMovementResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/edit")
    fun editGoodMovement(@RequestAttribute(WMS_SESSION) sessionId: String,
                           @RequestAttribute(USER_ID) userId: String,
                           @RequestBody request: CreateGoodMovementRequest): ResponseEntity<Any> {
        val result = editGoodMovementUseCase.execute(sessionId, userId, request)
        if (result is CreateGoodMovementResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }

    @GetMapping("/wms/remove/{goodMovementId}")
    fun removeGoodMovement(@RequestAttribute(WMS_SESSION) sessionId: String, @PathVariable goodMovementId: Int): ResponseEntity<Any> {
        val result = removeGoodMovementUseCase.execute(sessionId, goodMovementId)
        if (result is RemoveGoodMovementResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }

    @GetMapping("/wms/approve/{goodMovementId}")
    fun approveGoodMovement(@RequestAttribute(WMS_SESSION) sessionId: String, @PathVariable goodMovementId: Int): ResponseEntity<Any> {
        val result = approveGoodMovementUseCase.execute(sessionId, goodMovementId)
        if (result is RemoveGoodMovementResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }

    @GetMapping("/wms/{goodMovementId}")
    fun getGoodMovementDetail(@RequestAttribute(USER_ID) userId: String, @PathVariable goodMovementId: Int): ResponseEntity<Any> {
        val result = getGoodMovementDataUseCase.execute(userId, goodMovementId)
        if (result is GetGoodMovementDataResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }

    @GetMapping("/wms/pile/{goodMovementId}")
    fun getPileGoodMovementDetail(@RequestAttribute(USER_ID) userId: String, @PathVariable goodMovementId: Int): GetPileDetailsResponse {
        return getPileDetailsUseCase.execute(userId, goodMovementId)
    }

    @GetMapping("/wms/log/{goodMovementId}")
    fun getLogGoodMovementDetail(@PathVariable goodMovementId: Int): GetLogDetailsResponse {
        return getLogDetailsUseCase.execute(goodMovementId)
    }

    @GetMapping("/extraAttributes/{departmentId}")
    fun getExtraAttributes(@PathVariable departmentId: Int): GetExtraAttributesResponse {
        return getExtraAttributesUseCase.execute(departmentId)
    }

    @PostMapping("/wms/reference/add")
    fun addReferenceGoodMovement(@RequestAttribute(WMS_SESSION) sessionId: String,
                                 @RequestBody request: AddReferenceRequest): ResponseEntity<Any> {
        val result = addReferenceUseCase.execute(sessionId, request)
        if (result is ReferenceResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }

    @GetMapping("/wms/reference/remove/{pickingOrderGoodMovementId}")
    fun removeReferenceGoodMovement(@RequestAttribute(WMS_SESSION) sessionId: String,
                                    @PathVariable pickingOrderGoodMovementId: Int): ResponseEntity<Any> {
        val result = removeReferenceUseCase.execute(sessionId, pickingOrderGoodMovementId)
        if (result is ReferenceResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }

    @GetMapping("/wms/create/matchingGoodReceipt/{pickingGoodMovementId}")
    fun createMatchingGoodReceipt(@RequestAttribute(WMS_SESSION) sessionId: String,
                                  @RequestAttribute(USER_ID) userId: String,
                                  @PathVariable pickingGoodMovementId: Int): ResponseEntity<Any> {
        val result = createMatchingGoodReceiptUseCase.execute(sessionId, userId, pickingGoodMovementId)
        if (result is CreateGoodMovementResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/removeItem")
    fun removeGoodMovementItem(@RequestAttribute(WMS_SESSION) sessionId: String,
                               @RequestAttribute(USER_ID) userId: String,
                               @RequestBody request: RemoveGoodMovementItemRequest
    ) {
        removeGoodMovementItemUseCase.execute(sessionId, userId, request)
    }

    @PostMapping("/wms/create/wholeLine")
    fun createWholeProductLineGoodMovement(@RequestAttribute(WMS_SESSION) sessionId: String,
                                           @RequestAttribute(USER_ID) userId: String,
                                           @RequestBody request: CreateWholeProductLineGoodMovementRequest
    ): ResponseEntity<Any> {
        val result = createWholeProductLineGoodMovementUseCase.execute(sessionId, userId, request)
        if (result is CreateWholeProductLineGoodMovementResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }
}