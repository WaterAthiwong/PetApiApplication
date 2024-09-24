package com.champaca.inventorydata.log

enum class LogDeliveryError(val code: Int, val needToMark: Boolean) {
    NON_EXISTING_BARCODE(1, true),
    NON_EXISTING_MATCODE(2, true),
    ALREADY_IN_WMS(3, false),
    ALREADY_RECEIVED(4, false),
    NO_USERNAME_IN_WMS(5, false),
    NONE(0, false),
    LOCATION_NOT_FOUND(0, false),
    GOOD_MOVEMENT_NOT_FOUND(0, false),
    WMS_VALIDATION_ERROR(0, false);

    companion object {
        fun fromCode(code: Int?): LogDeliveryError? {
            return values().filter { it.code == code }.singleOrNull()
        }
    }
}