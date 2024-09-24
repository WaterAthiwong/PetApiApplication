package com.champaca.inventorydata.pile.response

import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.PileError
import java.math.BigDecimal

sealed class GetLotsResponse {
    data class Success(
        val pileId: Int,
        val pileCode: String,
        val goodMovementId: Int,
        val lots: List<MovingItem>,
        val skuGroup: NameIdPair,
        val species: NameIdPair,
        val fsc: Boolean,
        val orderNo: String?,
        val refNo: String?,
        val customer: String?,
        val remark: String?,
        val matCode: String?,
        val skuMainGroupName: String?,
        val thicknesses: List<BigDecimal>,
        val widths: List<BigDecimal>,
        val lengths: List<BigDecimal>,
        val grades: List<String>,
        val pileType : String
    ): GetLotsResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String?
    ): GetLotsResponse()

    data class NameIdPair(
        val code: String,
        val name: String
    )
}