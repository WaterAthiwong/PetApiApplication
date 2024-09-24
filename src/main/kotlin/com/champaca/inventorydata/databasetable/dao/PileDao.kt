package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.Pile
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class PileDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<PileDao>(Pile) {
        const val WOOD_PILE = "woodPile"
        const val SHELF = "shelf"
        const val ASSEMBLED_WOOD_PILE = "assemble"
        const val FG_BOX = "fgBox"
        const val REWORK = "rework"
        const val REJECTED = "rejected"
    }

    var manufacturingLineId by Pile.manufacturingLineId
    var goodMovementId by Pile.goodMovementId
    var originGoodMovementId by Pile.originGoodMovementId
    var storeLocationId by Pile.storeLocationId
    var code by Pile.code
    var processTypePrefix by Pile.processTypePrefix
    var lotSet by Pile.lotSet
    var type by Pile.type
    var orderNo by Pile.orderNo
    var remark by Pile.remark
    var extraAttributes by Pile.extraAttributes
    var printedAt by Pile.printedAt
    var countedAt by Pile.countedAt
    var countedUserId by Pile.countedUserId
    var createdAt by Pile.createdAt
    var updatedAt by Pile.updatedAt
    var status by Pile.status
    val goodMovement by GoodMovementDao referencedOn Pile.goodMovementId

    fun isActive(): Boolean {
        return status == "A"
    }
}