package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.GoodMovement
import com.champaca.inventorydata.databasetable.Pile
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class GoodMovementDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<GoodMovementDao>(GoodMovement) {
        const val TRANSFER_TO_CONDITION_ROOM = "TRANSFER_TO_CONDITION_ROOM"
    }
    var departmentId by GoodMovement.departmentId
    var goodReceiptGoodMovementId by GoodMovement.goodReceiptGoodMovementId
    var transferGoodMovementId by GoodMovement.transferGoodMovementId
    var manufacturingLineId by GoodMovement.manufacturingLineId
    var supplierId by GoodMovement.supplierId
    var userId by GoodMovement.userId
    var code by GoodMovement.code
    var type by GoodMovement.type
    var createdAt by GoodMovement.createdAt
    var orderNo by GoodMovement.orderNo
    var jobNo by GoodMovement.jobNo
    var poNo by GoodMovement.poNo
    var invoiceNo by GoodMovement.invoiceNo
    var lotNo by GoodMovement.lotNo
    var productionDate by GoodMovement.productionDate
    var remark by GoodMovement.remark
    var approveUserId by GoodMovement.approveUserId
    var closeUserId by GoodMovement.closeUserId
    var approvedAt by GoodMovement.approvedAt
    var closedAt by GoodMovement.closedAt
    var isTransfer by GoodMovement.isTransfer
    val extraAttributes by GoodMovement.extraAttributes
    var productType by GoodMovement.productType
    var status by GoodMovement.status
    val pile by PileDao referrersOn Pile.goodMovementId
}