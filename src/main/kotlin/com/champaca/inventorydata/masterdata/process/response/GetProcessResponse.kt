package com.champaca.inventorydata.masterdata.process.response

import com.champaca.inventorydata.masterdata.manufacturingline.model.ManufacturingLineData

data class GetProcessResponse(
    val processes: List<ProcessData>
) {
    data class ProcessData(
        val id: Int,
        val name: String,
        val manufacturingLines: List<ManufacturingLineData>
    )
}