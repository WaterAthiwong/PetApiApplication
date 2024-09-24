package com.champaca.inventorydata.masterdata.sku.fg

import com.champaca.inventorydata.masterdata.sku.model.FinishedGoodMatCodeQuery

interface FinishedGoodMatCodeComposer {
    fun compose(query: FinishedGoodMatCodeQuery): String
}