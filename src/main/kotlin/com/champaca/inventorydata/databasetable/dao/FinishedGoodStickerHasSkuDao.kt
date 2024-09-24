package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.FinishedGoodStickerHasSku
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class FinishedGoodStickerHasSkuDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<FinishedGoodStickerHasSkuDao>(FinishedGoodStickerHasSku)

    var stickerId by FinishedGoodStickerHasSku.stickerId
    var skuId by FinishedGoodStickerHasSku.skuId
    var qty by FinishedGoodStickerHasSku.qty
    var status by FinishedGoodStickerHasSku.status
}