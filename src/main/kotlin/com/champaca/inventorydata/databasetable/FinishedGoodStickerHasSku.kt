package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable

object FinishedGoodStickerHasSku: IntIdTable("cpc_fg_sticker_has_sku") {
    val stickerId = reference("cpc_fg_sticker_id", FinishedGoodSticker)
    val skuId = reference("sku_id", Sku)
    val qty = decimal("qty", 10, 2)
    val status = varchar("status", 1)
}