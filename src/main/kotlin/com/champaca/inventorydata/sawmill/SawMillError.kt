package com.champaca.inventorydata.sawmill

enum class SawMillError {
    BARCODE_NOT_FOUND,
    DUPLICATED_BARCODE,
    LOG_HAS_BEEN_USED,
    LOG_HAS_BEEN_BOOKED,
    WMS_VALIDATION_ERROR,
    GOOD_MOVEMENT_HAS_BEEN_APPROVED,
    NONE;
}