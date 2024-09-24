package com.champaca.inventorydata.masterdata.sku.fg

import com.champaca.inventorydata.masterdata.sku.model.FinishedGoodMatCodeQuery
import org.springframework.stereotype.Service

@Service
class FurnitureMatCodeComposer: FinishedGoodMatCodeComposer {
    override fun compose(query: FinishedGoodMatCodeQuery): String {
        val fsc = if (query.fsc) "1" else "2"

        val species = "${query.species}${query.secondLayerSpecies ?: ""}${query.thirdLayerSpecies ?: ""}${query.fourthLayerSpecies ?: ""}"

        return "5${query.skuGroupCode}${species}${fsc}-${query.furnitureCode}"
    }
}