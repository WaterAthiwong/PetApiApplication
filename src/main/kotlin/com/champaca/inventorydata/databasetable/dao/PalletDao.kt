package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.Pallet
import com.champaca.inventorydata.databasetable.Pile
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class PalletDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<PalletDao>(Pallet)
    var storeLocationId by Pallet.storeLocationId
    var code by Pallet.code
    var updatedAt by Pallet.updatedAt
    var createdAt by Pallet.createdAt
    var status by Pallet.status
    val piles by PileDao optionalReferrersOn Pile.palletId
}