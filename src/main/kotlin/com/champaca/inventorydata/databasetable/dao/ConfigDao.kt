package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.Config
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ConfigDao(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<ConfigDao>(Config)
    var name by Config.name
    var valueInt by Config.valueInt
    var valueString by Config.valueString
    var createdAt by Config.createdAt
}