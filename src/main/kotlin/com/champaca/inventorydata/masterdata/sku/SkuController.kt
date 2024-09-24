package com.champaca.inventorydata.masterdata.sku

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.common.ChampacaConstant
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.masterdata.model.SimpleData
import com.champaca.inventorydata.masterdata.sku.model.FinishedGoodMatCodeQuery
import com.champaca.inventorydata.masterdata.sku.model.MatCodeQuery
import com.champaca.inventorydata.masterdata.sku.model.SkuData
import com.champaca.inventorydata.masterdata.sku.request.CreateSkuFromErpRequest
import com.champaca.inventorydata.masterdata.sku.request.GetSkusRequest
import com.champaca.inventorydata.masterdata.sku.request.NonExistingSkusRequest
import com.champaca.inventorydata.masterdata.sku.response.CheckMatCodeExistResponse
import com.champaca.inventorydata.masterdata.sku.response.ImportSkusFromExcelFileResponse
import com.champaca.inventorydata.masterdata.sku.response.MatCodeAndSkuId
import com.champaca.inventorydata.masterdata.sku.response.NonExistingSkusResponse
import com.champaca.inventorydata.masterdata.sku.usecase.*
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.sql.DataSource

@RequestMapping("/sku")
@RestController
@CrossOrigin(origins = ["*"])
class SkuController(
    val skuRepository: SkuRepository,
    val dataSource: DataSource,
    val createSkuFromErpUseCase: CreateSkuFromErpUseCase,
    val checkMatCodeExistUseCase: CheckMatCodeExistUseCase,
    val checkFinishedGoodMatCodeExistUseCase: CheckFinishedGoodMatCodeExistUseCase,
    val getMatCodeByProductGroupUseCase: GetMatCodeByProductGroupUseCase,
    val getWasteMatCodesUseCase: GetWasteMatCodesUseCase,
    val importSkusFromExcelFileUseCase: ImportSkusFromExcelFileUseCase,
    val getSkusUseCase: GetSkusUseCase,
    val getFlooringPropertyUseCase: GetFlooringPropertyUseCase
) {

    @PostMapping(
        value = ["/nonExisting"], consumes = ["application/json"], produces = ["application/json"])
    fun findNonExistingMatCodes(@RequestBody request: NonExistingSkusRequest): NonExistingSkusResponse {
        var results: List<String> = listOf()
        Database.connect(dataSource)
        transaction {
            addLogger(ExposedInfoLogger)
            results = skuRepository.findNonExistingMatCodes(request.matCodes)
        }
        return NonExistingSkusResponse(results)
    }

    /**
     * A hidden API mean to disable duplicate SKUs and leave only the oldest Id active.
     * @return list of pair between mat code and disabled id.
     */
    @DeleteMapping("/deDupe")
    fun deleteDupe(): List<Pair<String, Int>> {
        var results: List<Pair<String, Int>> = listOf()
        Database.connect(dataSource)
        transaction {
            addLogger(ExposedInfoLogger)
            results = skuRepository.deleteDupe()
        }
        return results
    }

    @PostMapping("/wms/create")
    fun createSkusFromErp(@RequestAttribute(WmsService.WMS_SESSION) sessionId: String,
                          @RequestAttribute(ChampacaConstant.USER_ID) userId: String,
                          @RequestBody request: CreateSkuFromErpRequest): ResponseEntity<Any> {
        val result = createSkuFromErpUseCase.execute(sessionId, userId, request)
        return if (result is ResultOf.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body((result as ResultOf.Failure).message)
        }
    }

    @PostMapping("/matCode/exist")
    fun checkMatCodeExist(@RequestBody matCodeQuery: MatCodeQuery): CheckMatCodeExistResponse {
        return checkMatCodeExistUseCase.execute(matCodeQuery)
    }

    @PostMapping("/matCode/fg/exist")
    fun checkFinishedGoodMatCodeExist(@RequestBody matCodeQuery: FinishedGoodMatCodeQuery): CheckMatCodeExistResponse {
        return checkFinishedGoodMatCodeExistUseCase.execute(matCodeQuery)
    }

    @GetMapping("/matCode/group/{skuGroup}")
    fun getMatCodeByProductGroup(@PathVariable skuGroup: String): List<MatCodeAndSkuId> {
        return getMatCodeByProductGroupUseCase.execute(skuGroup)
    }

    @GetMapping("/waste")
    fun getWasteMatCodes(): List<MatCodeAndSkuId> {
        return getWasteMatCodesUseCase.execute()
    }

    @PostMapping("/import")
    fun importSkus(@RequestParam("file") file: MultipartFile): ResponseEntity<Any> {
        val result = importSkusFromExcelFileUseCase.execute(file)
        return if (result is ImportSkusFromExcelFileResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/import/fg")
    fun importFinishedGoodsSkus(@RequestParam("file") file: MultipartFile): ResponseEntity<Any> {
        val result = importSkusFromExcelFileUseCase.execute(file, true)
        return if (result is ImportSkusFromExcelFileResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/find")
    fun findSkus(@RequestBody request: GetSkusRequest): List<SkuData> {
        return getSkusUseCase.execute(request)
    }

    @GetMapping("/flooring/property/{propertyName}")
    fun getFlooringProperty(@PathVariable propertyName: String): List<SimpleData> {
        return getFlooringPropertyUseCase.execute(propertyName)
    }
}