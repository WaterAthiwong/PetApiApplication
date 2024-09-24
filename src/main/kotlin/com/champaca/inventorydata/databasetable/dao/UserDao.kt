package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.User
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UserDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<UserDao>(User)
    var companyId by User.companyId
    var processTypeId by User.processTypeId
    var username by User.username
    var password by User.password
    var firstname by User.firstname
    var lastname by User.lastname
    var phone by User.phone
    var email by User.email
    var facebookId by User.facebookId
    var picture by User.picture
    var birthdate by User.birthdate
    var sex by User.sex
    var ip by User.ip
    var createdAt by User.createdAt
    var lang by User.lang
    var changePasswordAt by User.changePasswordAt
    var status by User.status
}