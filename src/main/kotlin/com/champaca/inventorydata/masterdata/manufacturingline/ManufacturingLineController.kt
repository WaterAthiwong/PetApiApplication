package com.champaca.inventorydata.masterdata.manufacturingline

import com.champaca.inventorydata.masterdata.manufacturingline.model.ManufacturingLineData
import com.champaca.inventorydata.masterdata.manufacturingline.request.GetManufacturingLineRequest
import com.champaca.inventorydata.masterdata.manufacturingline.usecase.GetManufacturingLineUseCase
import com.champaca.inventorydata.wms.WmsService
import org.springframework.web.bind.annotation.*

@RequestMapping("/manufacturingLine")
@RestController
@CrossOrigin(origins = ["*"])
class ManufacturingLineController(
    val getManufacturingLineUseCase: GetManufacturingLineUseCase
) {

    @PostMapping("/wms/")
    fun getManufacturingLine(@RequestAttribute(WmsService.WMS_SESSION) sessionId: String, @RequestBody request: GetManufacturingLineRequest): List<ManufacturingLineData> {
        return getManufacturingLineUseCase.execute(sessionId, request)
    }
}