package com.champaca.inventorydata.costing

import com.champaca.inventorydata.costing.model.RawMaterialCostData
import com.champaca.inventorydata.costing.request.GetRawMaterialCostsRequest
import com.champaca.inventorydata.costing.response.UploadLogCostResponse
import com.champaca.inventorydata.costing.usecase.GetRawMaterialCostsUseCase
import com.champaca.inventorydata.costing.usecase.UploadLogCostUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RequestMapping("/costing/rm")
@RestController
@CrossOrigin(origins = ["*"])
class RawMaterialCostController(
    val getRawMaterialCostsUseCase: GetRawMaterialCostsUseCase,
    val uploadLogCostUseCase: UploadLogCostUseCase
) {

    @PostMapping("/")
    fun getRawMaterialCosts(@RequestBody request: GetRawMaterialCostsRequest): List<RawMaterialCostData> {
        return getRawMaterialCostsUseCase.execute(request)
    }

    @PostMapping("/log/upload")
    fun uploadLogCost(@RequestParam("file") file: MultipartFile, @RequestParam("supplierId") supplierId: Int): ResponseEntity<Any> {
        val response = uploadLogCostUseCase.execute(file, supplierId)
        return if (response is UploadLogCostResponse.Success) {
            ResponseEntity.ok().body(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

}