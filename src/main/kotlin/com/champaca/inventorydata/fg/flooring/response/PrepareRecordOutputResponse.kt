package com.champaca.inventorydata.fg.flooring.response

import com.champaca.inventorydata.fg.flooring.model.MatCodeAttributeChangeType
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.request.PileItem

sealed class PrepareRecordOutputResponse {
    data class Success(
        val changes: List<MatCodeAttributeChangeType>,
        val location: String,
        val items: List<PileItem>,
        val newGroups: List<String>
    ): PrepareRecordOutputResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = null
    ): PrepareRecordOutputResponse()
}