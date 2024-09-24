package com.champaca.inventorydata.fg.flooring.model

enum class MatCodeAttributeChangeType(
    val sameValueForAllItems: Boolean
) {
    NEW_THICKNESS(false),
    NEW_WIDTH(false),
    NEW_LENGTH(false),
    INCREASE_MAIN_SKU_GROUP_FROM_3_TO_4(true),
    NEW_SKU_GROUP(true);
}