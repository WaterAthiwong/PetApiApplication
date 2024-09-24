package com.champaca.inventorydata.masterdata.sku.response

import com.champaca.inventorydata.masterdata.sku.model.SkuData
import com.champaca.inventorydata.pile.PileError

sealed class ImportSkusFromExcelFileResponse {
    data class Success(
        val existings: List<SkuData>,
        val createds: List<SkuData>
    ): ImportSkusFromExcelFileResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): ImportSkusFromExcelFileResponse()
}
