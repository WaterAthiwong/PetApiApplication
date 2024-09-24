package com.champaca.inventorydata.goodmovement.model

enum class GoodMovementType(val wmsName: String) {
    PICKING_ORDER("picking order"),
    GOODS_RECEIPT("good receipt"),
    NONE("");

    companion object {
        fun fromString(str: String): GoodMovementType {
            return when (str) {
                "picking order" -> PICKING_ORDER
                "good receipt" -> GOODS_RECEIPT
                else -> NONE
            }
        }
    }
}