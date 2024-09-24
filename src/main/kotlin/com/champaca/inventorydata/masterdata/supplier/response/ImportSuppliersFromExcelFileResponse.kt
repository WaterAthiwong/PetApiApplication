package com.champaca.inventorydata.masterdata.supplier.response

import com.champaca.inventorydata.masterdata.sku.model.SkuData
import com.champaca.inventorydata.masterdata.supplier.model.SupplierData
import com.champaca.inventorydata.pile.PileError

sealed class ImportSuppliersFromExcelFileResponse {
    data class Success(
        val existings: List<SupplierData>,
        val createds: List<SupplierData>
    ): ImportSuppliersFromExcelFileResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): ImportSuppliersFromExcelFileResponse()
}
