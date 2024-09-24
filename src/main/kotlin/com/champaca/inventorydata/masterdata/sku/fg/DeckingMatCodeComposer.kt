package com.champaca.inventorydata.masterdata.sku.fg

import com.champaca.inventorydata.masterdata.sku.model.FinishedGoodMatCodeQuery
import org.springframework.stereotype.Service

@Service
class DeckingMatCodeComposer: FinishedGoodMatCodeComposer {
    override fun compose(query: FinishedGoodMatCodeQuery): String {
        val fsc = if (query.fsc) "1" else "2"

        val color = if (query.color.isNullOrEmpty()) {
            "NNN"
        } else {
            "${query.color}"
        }

        val species = "${query.species}${query.secondLayerSpecies ?: ""}${query.thirdLayerSpecies ?: ""}${query.fourthLayerSpecies ?: ""}"
        val dimension = "${query.thickness}X${query.width}X${query.length}"

        return "5${query.skuGroupCode}${species}${fsc}-${dimension}-${color}"
    }
}