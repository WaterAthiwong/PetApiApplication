package com.champaca.inventorydata.pile

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.champaca.inventorydata.common.ChampacaConstant.USER_ID
import com.champaca.inventorydata.pile.request.*
import com.champaca.inventorydata.pile.response.*
import com.champaca.inventorydata.pile.usecase.*
import com.champaca.inventorydata.wms.WmsService.Companion.WMS_SESSION

@RequestMapping("/pile")
@RestController
@CrossOrigin(origins = ["*"])
class PileController(
    val createPileUseCase: CreatePileUseCase,
    val relocatePileUseCase: RelocatePileUseCase,
    val pickPileUseCase: PickPileUseCase,
    val receivePileUseCase: ReceivePileUseCase,
    val getLotsUseCase: GetLotsUseCase,
    val partialPickUseCase: PartialPickUseCase,
    val getPileDetailUseCase: GetPileDetailUseCase,
    val moveToPalletUseCase: MoveToPalletUseCase,
    val editPileUseCase: EditPileUseCase,
    val getPilesUseCase: GetPilesUseCase,
    val getPilesPrintUseCase: GetPilesPrintUseCase,
    val removePileUseCase: RemovePileUseCase,
    val importExistingPileUseCase: ImportExistingPileUseCase,
    val undoPileUseCase: UndoPileUseCase,
    val stockCountUseCase: StockCountUseCase
) {
    @PostMapping("/wms/create")
    fun create(@RequestAttribute(WMS_SESSION) sessionId: String,
               @RequestAttribute(USER_ID) userId: String,
               @RequestBody request: CreatePileRequest): ResponseEntity<Any> {
        val result = createPileUseCase.execute(sessionId, userId, request)
        return if (result is CreatePileResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/relocate")
    fun relocate(@RequestAttribute(WMS_SESSION) sessionId: String,
                 @RequestAttribute(USER_ID) userId: String,
                 @RequestBody request: RelocatePileRequest): ResponseEntity<Any> {
        val result = relocatePileUseCase.execute(sessionId, userId, request)
        return if (result is RelocatePileResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/pick")
    fun pickForProcess(@RequestAttribute(WMS_SESSION) sessionId: String,
                       @RequestAttribute(USER_ID) userId: String,
                       @RequestBody request: PickPileRequest): ResponseEntity<Any> {
        val result = pickPileUseCase.execute(sessionId, userId, request)
        return if (result is PickPileResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/partialPick")
    fun partialPick(@RequestAttribute(WMS_SESSION) sessionId: String,
                    @RequestAttribute(USER_ID) userId: String,
                    @RequestBody request: PartialPickRequest): ResponseEntity<Any> {
        val result = partialPickUseCase.execute(sessionId, userId, request)
        return if (result is PartialPickResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/receive")
    fun receiveFromProcess(@RequestAttribute(WMS_SESSION) sessionId: String,
                           @RequestAttribute(USER_ID) userId: String,
                           @RequestBody request: ReceivePileRequest): ResponseEntity<Any> {
        val result = receivePileUseCase.execute(sessionId, userId, request)
        return if (result is ReceivePileResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @GetMapping("/wms/lots/{pileCode}")
    fun getLots(@PathVariable pileCode: String): ResponseEntity<Any> {
        val result = getLotsUseCase.execute(pileCode)
        return if (result is GetLotsResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @GetMapping("/wms/lots/items/{pileCode}")
    fun getItems(@PathVariable pileCode: String): ResponseEntity<Any> {
        val result = getLotsUseCase.execute(pileCode, true)
        return if (result is GetLotsResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @GetMapping("/wms/{pileCode}")
    fun getDetail(@PathVariable pileCode: String): ResponseEntity<Any> {
        val result = getPileDetailUseCase.execute(pileCode)
        return if (result is GetPileDetailResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/edit")
    fun edit(@RequestAttribute(WMS_SESSION) sessionId: String,
             @RequestAttribute(USER_ID) userId: String,
             @RequestBody request: EditPileRequest): ResponseEntity<Any> {
        val result = editPileUseCase.execute(sessionId, userId, request)
        return if (result is EditPileResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/pallet/move")
    fun moveToPallet(@RequestAttribute(USER_ID) userId: String,
                    @RequestBody request: MoveToPalletRequest): ResponseEntity<Any> {
        val result = moveToPalletUseCase.execute(userId, request)
        return if (result is MoveToPalletResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/getpiles")
    fun getPileByDateLine(@RequestAttribute(USER_ID) userId: String, @RequestBody request: GetPileByDateLine): ResponseEntity<Any> {
        val result: GetPileByDateLineResponse = getPilesUseCase.execute(userId, request)
        return  if (result is GetPileByDateLineResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/getpiles/print")
    fun printPileSheet(@RequestAttribute(USER_ID) userId: String, @RequestBody request: PrintPileRequest): ResponseEntity<out Any> {
        val result: GetPilesLotsResponse = getPilesPrintUseCase.execute(userId, request)
        return if (result is GetPilesLotsResponse.Success) {
            getPilesPrintUseCase.ReportjasperSetting(result, request)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/remove")
    fun remove(@RequestAttribute(WMS_SESSION) sessionId: String,
               @RequestAttribute(USER_ID) userId: String,
               @RequestBody request: RemovePileRequest): ResponseEntity<Any> {
        val result = removePileUseCase.execute(sessionId, userId, request)
        return if (result is RemovePileResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/import")
    fun import(@RequestAttribute(WMS_SESSION) sessionId: String,
               @RequestAttribute(USER_ID) userId: String,
               @RequestBody request: ImportExistingPileRequest): String {
        return importExistingPileUseCase.execute(sessionId, userId, request)
    }

    @PostMapping("/wms/undo")
    fun undo(@RequestAttribute(WMS_SESSION) sessionId: String,
               @RequestAttribute(USER_ID) userId: String,
               @RequestBody request: UndoPileRequest): ResponseEntity<Any> {
        val result = undoPileUseCase.execute(sessionId, userId, request)
        return if (result is UndoPileResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/stockCount")
    fun stockCount(@RequestAttribute(USER_ID) userId: String, @RequestBody request: StockCountRequest): ResponseEntity<Any> {
        val result = stockCountUseCase.execute(userId, request)
        return if (result is StockCountResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }
}