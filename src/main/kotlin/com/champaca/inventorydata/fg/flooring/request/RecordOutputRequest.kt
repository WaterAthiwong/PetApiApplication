package com.champaca.inventorydata.fg.flooring.request

import com.champaca.inventorydata.pile.request.PileItem
import java.math.BigDecimal

data class RecordOutputRequest(
    val pileCode: String,
    val goodMovementId: Int,
    val manufacturingLineId: Int?,
    val processTypeId: Int,
    val processPrefix: String,
    val location: String,
    val items: List<PileItem>,
    val newGroup: String = "",
    val processingResults: List<ProcessingResult>
) {
    data class ProcessingResult(
        val finishedQty: BigDecimal,
        val reworkQty: BigDecimal,
        val newThickness: String = "",
        val newWidth: String = "",
        val newLength: String = ""
    )
}