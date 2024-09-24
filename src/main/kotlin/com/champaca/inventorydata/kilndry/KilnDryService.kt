package com.champaca.inventorydata.kilndry

import com.champaca.inventorydata.common.ProcessedWoodCommonService
import com.champaca.inventorydata.kilndry.request.GetStockRequest
import com.champaca.inventorydata.model.ProcessedWood
import org.springframework.stereotype.Service

@Service
class KilnDryService(
    val processedWoodCommonService: ProcessedWoodCommonService
) {

    fun getStock(request: GetStockRequest): List<ProcessedWood> {
        return processedWoodCommonService.getStockInProcess("KD", request.kilnNos ?: listOf())
    }
}