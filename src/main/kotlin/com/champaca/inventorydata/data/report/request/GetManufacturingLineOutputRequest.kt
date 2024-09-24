package com.champaca.inventorydata.data.report.request

data class GetManufacturingLineOutputRequest(
    val departmentId: Int,
    val manufacturingLineIds: List<Int> = emptyList(),
    val fromProductionDate: String,
    val toProductionDate: String,
    val pick: Boolean,
    val receive: Boolean
) {
    fun isBlank(): Boolean {
        return departmentId == 0 && manufacturingLineIds.isEmpty() && fromProductionDate.isBlank() &&
                toProductionDate.isBlank() && !pick && !receive
    }
}
