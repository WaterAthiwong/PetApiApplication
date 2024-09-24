package com.champaca.inventorydata.cleansing

import com.champaca.inventorydata.cleansing.request.MassImportStockRequest
import com.champaca.inventorydata.cleansing.usecase.*
import com.champaca.inventorydata.common.ChampacaConstant
import com.champaca.inventorydata.wms.WmsService
import org.springframework.web.bind.annotation.*

@RequestMapping("/cleansing")
@RestController
@CrossOrigin(origins = ["*"])
class DataCleansingController(
    val reviseLogExtraAttributesUseCase: ReviseLogExtraAttributesUseCase,
    val receiveMultipleLogs: ReceiveMultipleLogs,
    val fillProductTypeUseCase: FillProductTypeUseCase,
    val removeNoTagUseCase: RemoveNoTagUseCase,
    val massImportStockUseCase: MassImportStockUseCase,
    val checkLotStockUseCase: CheckLotStockUseCase,
    val checkSkuVolumnUseCase: CheckSkuVolumnUseCase,
    val checkSkuAreaUseCase: CheckSkuAreaUseCase,
    val massConvertToMillimeterUseCase: MassConvertToMillimeterUseCase,
    val createItemFileForOdooUseCase: CreateItemFileForOdooUseCase
) {
    @GetMapping("/reviseLog")
    fun reviseLogExtraAttributes() {
        reviseLogExtraAttributesUseCase.execute()
    }

    @PostMapping("/receiveMultipleLogs")
    fun receiveMultipleLogs(@RequestBody refCodes: List<String>) {
        receiveMultipleLogs.execute("natachart.la", refCodes)
    }

    @GetMapping("/fillProductType")
    fun fillProductType() {
        fillProductTypeUseCase.execute()
    }

    @GetMapping("/wms/removeNoTag")
    fun removeNoTag(@RequestAttribute(WmsService.WMS_SESSION) sessionId: String) {
        removeNoTagUseCase.execute(sessionId)
    }

    @PostMapping("/wms/massImportStock")
    fun massImportStock(@RequestAttribute(WmsService.WMS_SESSION) sessionId: String,
                        @RequestAttribute(ChampacaConstant.USER_ID) userId: String,
                        @RequestBody request: MassImportStockRequest
    ) {
        massImportStockUseCase.execute(sessionId, userId, request)
    }

    @PostMapping("/checkLotStock")
    fun checkLotStock(@RequestBody lotNoIds: List<Int>) {
        checkLotStockUseCase.execute(lotNoIds)
    }

    @GetMapping("/checkSkuVolumn")
    fun checkSkuVolumn(): List<String> {
        return checkSkuVolumnUseCase.execute()
    }

    @GetMapping("/checkSkuArea")
    fun checkSkuArea(): List<String> {
        return checkSkuAreaUseCase.execute()
    }

    @GetMapping("/massConvert")
    fun massConvert() {
        return massConvertToMillimeterUseCase.execute()
    }

    @GetMapping("/odoo")
    fun odoo() {
        createItemFileForOdooUseCase.execute()
    }
}