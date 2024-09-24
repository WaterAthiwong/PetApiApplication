package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.FinishedGoodStickerBatch
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class FinishedGoodStickerBatchDao(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<FinishedGoodStickerBatchDao>(FinishedGoodStickerBatch)

    var suppilerId by FinishedGoodStickerBatch.suppilerId
    var code by FinishedGoodStickerBatch.code
    var salesOrderNo by FinishedGoodStickerBatch.salesOrderNo
    var salesOrderLineNo by FinishedGoodStickerBatch.salesOrderLineNo
    var format by FinishedGoodStickerBatch.format
    var productionDate by FinishedGoodStickerBatch.productionDate
    var remark by FinishedGoodStickerBatch.remark
    var remark2 by FinishedGoodStickerBatch.remark2
    var createdAt by FinishedGoodStickerBatch.createdAt
    var status by FinishedGoodStickerBatch.status
}