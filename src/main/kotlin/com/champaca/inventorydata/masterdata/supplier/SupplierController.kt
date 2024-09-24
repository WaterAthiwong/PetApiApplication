package com.champaca.inventorydata.masterdata.supplier

import com.champaca.inventorydata.masterdata.sku.response.ImportSkusFromExcelFileResponse
import com.champaca.inventorydata.masterdata.supplier.usecase.GetSupplierUseCase
import com.champaca.inventorydata.masterdata.supplier.model.SupplierData
import com.champaca.inventorydata.masterdata.supplier.response.ImportSuppliersFromExcelFileResponse
import com.champaca.inventorydata.masterdata.supplier.usecase.ImportSupplierFromExcelFileUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RequestMapping("/supplier")
@RestController
@CrossOrigin(origins = ["*"])
class SupplierController(
    val getSupplierUseCase: GetSupplierUseCase,
    val importSupplierFromExcelFileUseCase: ImportSupplierFromExcelFileUseCase
) {

    @GetMapping("/")
    fun getAll(): List<SupplierData> {
        return getSupplierUseCase.execute(null)
    }

    @GetMapping("/{type}")
    fun getByType(@PathVariable type: String): List<SupplierData> {
        // type shall be one of the const values in SupplierDao
        // (INTERNAL, FORESTRY, CUSTOMER, SUPPLIER)
        return getSupplierUseCase.execute(type)
    }

    @PostMapping("/import")
    fun importSkus(@RequestParam("file") file: MultipartFile): ResponseEntity<Any> {
        val result = importSupplierFromExcelFileUseCase.execute(file)
        return if (result is ImportSuppliersFromExcelFileResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }
}