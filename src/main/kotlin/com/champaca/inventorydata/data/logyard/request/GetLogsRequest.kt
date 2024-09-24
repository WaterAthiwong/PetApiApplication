package com.champaca.inventorydata.data.logyard.request

import com.champaca.inventorydata.log.StoredLogSearchParam

data class GetLogsRequest(
    val minLength: Double?,
    val minCircumference: Double?,
    val suppliers: List<Int>?
) {
    fun toStoredLogSearchParam(): StoredLogSearchParam {
        return StoredLogSearchParam(
            minLength = this.minLength,
            minCircumference = this.minCircumference,
            suppliers = this.suppliers
        )
    }
}