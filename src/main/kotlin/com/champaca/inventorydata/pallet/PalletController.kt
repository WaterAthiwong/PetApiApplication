package com.champaca.inventorydata.pallet

import com.champaca.inventorydata.common.ChampacaConstant
import com.champaca.inventorydata.pallet.request.PickPalletRequest
import com.champaca.inventorydata.pallet.response.PickPalletResponse
import com.champaca.inventorydata.pallet.usecase.CreatePalletUseCase
import com.champaca.inventorydata.pallet.usecase.PickPalletUseCase
import com.champaca.inventorydata.wms.WmsService
import org.apache.catalina.connector.Response
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RequestMapping("/pallet")
@RestController
@CrossOrigin(origins = ["*"])
class PalletController(
    val createPalletUseCase: CreatePalletUseCase,
    val pickPalletUseCase: PickPalletUseCase
) {

    @GetMapping("/create/{processPrefix}")
    fun create(@PathVariable processPrefix: String): String {
        return createPalletUseCase.execute(processPrefix)
    }

    @PostMapping("/wms/pick")
    fun pick(@RequestAttribute(WmsService.WMS_SESSION) sessionId: String,
             @RequestAttribute(ChampacaConstant.USER_ID) userId: String,
             @RequestBody request: PickPalletRequest): ResponseEntity<Any> {
        val result = pickPalletUseCase.execute(sessionId, userId, request)
        return if (result is PickPalletResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }
}