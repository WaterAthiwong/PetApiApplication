package com.champaca.inventorydata.pile.response

import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.model.PickedGoodMovement
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed class GetPileDetailResponse{
    data class Success(
        val status: String,
        val goodMovementId: Int,
        val jobNo: String?,
        val remark: String?,
        val items: List<Item>,
        val totalInitial: BigDecimal,
        val totalCurrent: BigDecimal,
        val timelines: List<Timeline>,
        val currentLocation: String?,
        val orderNo: String?,
    ): GetPileDetailResponse()

    data class Failure(
        val errorType: PileError,
        val errorMessage: String? = ""
    ): GetPileDetailResponse()

    @JsonIgnoreProperties("localDateTime")
    data class Timeline(
        val description: String,
        val localDateTime: LocalDateTime,
        val name: String,
        val undone: Boolean
    ) {
        companion object {
            val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        }

        val dateTime: String
            get() = localDateTime.format(FORMAT)
    }

    data class Item(
        val initialMatCode: String,
        val currentMatCode: String,
        val skuName: String,
        val initialQty: BigDecimal,
        val currentQty: BigDecimal
    )
}