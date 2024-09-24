package com.champaca.inventorydata.databasetable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDate
import java.time.LocalDateTime

object User: IntIdTable("user") {
    val companyId: Column<Int> = integer("company_id")
    val processTypeId: Column<Int> = integer("process_type_id")
    val username : Column<String> = varchar("username", 45)
    val password : Column<String> = varchar("password", 45)
    val firstname : Column<String> = varchar("firstname", 45)
    val lastname : Column<String> = varchar("lastname", 45)
    val phone : Column<String> = varchar("phone", 45)
    val email : Column<String> = varchar("email", 45)
    val facebookId : Column<String> = varchar("facebook_id", 45)
    val picture : Column<String> = text("picture")
    val birthdate : Column<LocalDate> = date("birthdate")
    val sex : Column<String> = varchar("sex", 10)
    val ip : Column<String> = varchar("ip", 45)
    val createdAt : Column<LocalDateTime> = datetime("created_at")
    val lang : Column<String> = varchar("lang", 45)
    val changePasswordAt : Column<LocalDateTime> = datetime("change_password_at")
    val status : Column<String> = varchar("status", 1)
}