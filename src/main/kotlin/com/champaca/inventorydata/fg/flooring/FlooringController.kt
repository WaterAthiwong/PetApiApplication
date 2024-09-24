package com.champaca.inventorydata.fg.flooring

import com.champaca.inventorydata.common.ChampacaConstant
import com.champaca.inventorydata.fg.flooring.model.BatchData
import com.champaca.inventorydata.fg.flooring.model.StickerData
import com.champaca.inventorydata.fg.flooring.request.*
import com.champaca.inventorydata.fg.flooring.response.*
import com.champaca.inventorydata.fg.flooring.usecase.*
import com.champaca.inventorydata.pile.response.ReceivePileResponse
import com.champaca.inventorydata.wms.WmsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RequestMapping("/fg/flooring")
@RestController
@CrossOrigin(origins = ["*"])
class FlooringController(
    val createStickerBatchUseCase: CreateStickerBatchUseCase,
    val getBatchUseCase: GetBatchesUseCase,
    val registerBoxUseCase: RegisterBoxUseCase,
    val getStickerDetialsUseCase: GetStickerDetialsUseCase,
    val switchStickerBatchUseCase: SwitchStickerBatchUseCase,
    val editStickerQtyUseCase: EditStickerQtyUseCase,
    val addStickerToBatchUseCase: AddStickerToBatchUseCase,
    val prepareRecordOutputUseCase: PrepareRecordOutputUseCase,
    val recordOutputUseCase: RecordOutputUseCase,
    val joinPileUseCase: JoinPileUseCase,
    val recordStickerPrintedUseCase: RecordStickerPrintedUseCase
) {

    @PostMapping("/sticker/batch")
    fun getBatches(@RequestBody request: GetBatchesRequest): List<BatchData> {
        return getBatchUseCase.execute(request)
    }

    @PostMapping("/sticker/batch/create")
    fun createStickerBatch(@RequestBody request: CreateStickerBatchRequest):  ResponseEntity<Any> {
        val result = createStickerBatchUseCase.execute(request)
        if (result is CreateStickerBatchResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/box/register")
    fun registerBox(@RequestAttribute(WmsService.WMS_SESSION) sessionId: String,
                    @RequestAttribute(ChampacaConstant.USER_ID) userId: String,
                    @RequestBody request: RegisterBoxRequest):  ResponseEntity<Any> {
        val result = registerBoxUseCase.execute(sessionId, userId, request)
        if (result is RegisterBoxResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/sticker/details")
    fun getStickerDetails(@RequestBody request: GetStickerDetailsRequest): List<StickerData> {
        return getStickerDetialsUseCase.execute(request)
    }

    @PostMapping("/sticker/batch/switch")
    fun switchStickerBatch(@RequestBody request: SwitchStickerBatchRequest): ResponseEntity<Any> {
        val result = switchStickerBatchUseCase.execute(request)
        if (result is SwitchStickerBatchResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/sticker/printed")
    fun recordStickerPrinted(@RequestBody request: RecordStickerPrintedRequest): ResponseEntity<Any> {
        val result = recordStickerPrintedUseCase.execute(request)
        if (result is RecordStickerPrintedResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/sticker/edit")
    fun editStickerQty(@RequestBody request: EditStickerQtyRequest): ResponseEntity<Any> {
        val result = editStickerQtyUseCase.execute(request)
        if (result is EditStickerQtyResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/sticker/batch/add")
    fun addStickerToBatch(@RequestBody request: AddStickerToBatchRequest): ResponseEntity<Any> {
        val result = addStickerToBatchUseCase.execute(request)
        if (result is AddStickerToBatchResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }


    @PostMapping("/output/prepare")
    fun prepareRecordOutput(@RequestBody request: PrepareRecordOutputRequest): ResponseEntity<Any> {
        val result = prepareRecordOutputUseCase.execute(request)
        if (result is PrepareRecordOutputResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/output/record")
    fun recordOutput(@RequestAttribute(WmsService.WMS_SESSION) sessionId: String,
                     @RequestAttribute(ChampacaConstant.USER_ID) userId: String,
                     @RequestBody request: RecordOutputRequest): ResponseEntity<Any> {
        val result = recordOutputUseCase.execute(sessionId, userId, request)
        if (result is RecordOutputResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/pile/join")
    fun joinPile(@RequestAttribute(ChampacaConstant.USER_ID) userId: String,
                 @RequestBody request: JoinPileRequest): ResponseEntity<Any> {
        val result = joinPileUseCase.execute(userId, request)
        if (result is JoinPileResponse.Success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.badRequest().body(result)
        }
    }
}