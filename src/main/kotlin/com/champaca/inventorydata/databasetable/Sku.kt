package com.champaca.inventorydata.databasetable

import com.champaca.inventorydata.databasetable.columntype.ynBool
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.math.BigDecimal
import java.time.LocalDateTime

object Sku: IntIdTable("sku") {
    val format = Json { prettyPrint = true }

    val skuGroupId: Column<Int> = integer("sku_group_id")
    val skuId: Column<Int?> = integer("sku_id").nullable()
    val additionalFieldId: Column<Int?> = integer("additional_field_id").nullable()
    val erpCode: Column<String?> = varchar("erp_code", 45).nullable()
    val code : Column<String> = varchar("code", 45)
    val matCode : Column<String> = varchar("mat_code", 45)
    val name : Column<String> = varchar("name", 255)
    val thickness : Column<BigDecimal> = decimal("thickness", 12, 5)
    val width : Column<BigDecimal> = decimal("width", 12, 5)
    val length : Column<BigDecimal> = decimal("long", 12, 5)
    val widthUom : Column<String> = varchar("width_uom", 45)
    val lengthUom : Column<String> = varchar("long_uom", 45)
    val thicknessUom : Column<String> = varchar("thickness_uom", 45)
    val circumference : Column<BigDecimal> = decimal("circumference", 12, 5)
    val circumferenceUom : Column<String> = varchar("circumference_uom", 45)
    val volumnM3 : Column<BigDecimal> = decimal("volumn_m3", 12, 5)
    val volumnFt3 : Column<BigDecimal> = decimal("volumn_ft3", 12, 5)
    val areaM2 : Column<BigDecimal?> = decimal("area_m2", 12, 5).nullable()
    val species : Column<String> = varchar("species", 45)
    val grade : Column<String?> = varchar("grade", 4).nullable()
    val fsc : Column<Boolean> = ynBool("fsc")
    val extraAttributes: Column<Map<String, String>?> = json<Map<String, String>>("extra_attributes", Pile.format).nullable()
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val updatedAt: Column<LocalDateTime> = datetime("updated_at")
    val status : Column<String> = varchar("status", 1)
}