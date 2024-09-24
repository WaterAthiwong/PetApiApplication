package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.FinishedGoodSticker
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class FinishedGoodStickerDao(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<FinishedGoodStickerDao>(FinishedGoodSticker)

    var batchId by FinishedGoodSticker.batchId
    var pileId by FinishedGoodSticker.pileId
    var code by FinishedGoodSticker.code
    var isFragment by FinishedGoodSticker.isFragment
    var createdAt by FinishedGoodSticker.createdAt
    var updatedAt by FinishedGoodSticker.updatedAt
    var printedAt by FinishedGoodSticker.printedAt
    var status by FinishedGoodSticker.status
}