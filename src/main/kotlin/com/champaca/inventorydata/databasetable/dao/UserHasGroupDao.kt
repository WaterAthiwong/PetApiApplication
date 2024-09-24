package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.UserHasGroup
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UserHasGroupDao(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<UserHasGroupDao>(UserHasGroup)
    var userId by UserHasGroup.userId
    var groupId by UserHasGroup.groupId
}