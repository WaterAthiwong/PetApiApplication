package com.champaca.inventorydata.sawmill

import com.champaca.inventorydata.common.ChampacaConstant
import com.champaca.inventorydata.common.ProcessedWoodCommonService
import com.champaca.inventorydata.model.ProcessedWood
import com.champaca.inventorydata.sawmill.request.CancelBookingRequest
import com.champaca.inventorydata.sawmill.request.GetManufacturedRequest
import com.champaca.inventorydata.sawmill.request.SawRequest
import com.champaca.inventorydata.sawmill.response.CancelBookingResponse
import com.champaca.inventorydata.sawmill.response.SawResponse
import com.champaca.inventorydata.sawmill.usecase.BookForSawUseCase
import com.champaca.inventorydata.sawmill.usecase.CancelBookingUseCase
import com.champaca.inventorydata.sawmill.usecase.SawUseCase
import com.champaca.inventorydata.wms.WmsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RequestMapping("/sawmill")
@RestController
@CrossOrigin(origins = ["*"])
class SawMillController(
    val processedWoodCommonService: ProcessedWoodCommonService,
    val sawUseCase: SawUseCase,
    val bookForSawUseCase: BookForSawUseCase,
    val cancelBookingUseCase: CancelBookingUseCase
) {
    companion object {
        const val SAWMILL_PREFIX = "SM"
    }

    @PostMapping(
        value = ["/manufactured"], consumes = ["application/json"], produces = ["application/json"])
    fun getManufacturedWood(@RequestBody request: GetManufacturedRequest): List<ProcessedWood> {
        val codes: List<String> = request.goodsMovementCodes ?: listOf()
        val manuLines: List<String> = request.manufacturingLines ?: listOf()
        val startDate: String = request.startDate ?: ""
        val endDate: String = request.endDate ?: ""
        return processedWoodCommonService.getManufacturedItemsFromProcess(SAWMILL_PREFIX, goodMovementCodes =codes,
            manuLines = manuLines,
            startDate=startDate,
            endDate=endDate)
    }

    @PostMapping("/wms/saw")
    fun saw(@RequestAttribute(WmsService.WMS_SESSION) sessionId: String,
            @RequestAttribute(ChampacaConstant.USER_ID) userId: String,
            @RequestBody request: SawRequest): ResponseEntity<Any> {
        val result = sawUseCase.execute(sessionId, userId, request)
        return if (result is SawResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/book")
    fun book(@RequestAttribute(WmsService.WMS_SESSION) sessionId: String,
             @RequestAttribute(ChampacaConstant.USER_ID) userId: String,
             @RequestBody request: SawRequest): ResponseEntity<Any> {
        val result = bookForSawUseCase.execute(sessionId, userId, request)
        return if (result is SawResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/wms/cancel")
    fun cancelBooking(@RequestAttribute(WmsService.WMS_SESSION) sessionId: String,
                      @RequestAttribute(ChampacaConstant.USER_ID) userId: String,
                      @RequestBody request: CancelBookingRequest): ResponseEntity<Any> {
        val result = cancelBookingUseCase.execute(sessionId, userId, request)
        return if (result is CancelBookingResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }
}