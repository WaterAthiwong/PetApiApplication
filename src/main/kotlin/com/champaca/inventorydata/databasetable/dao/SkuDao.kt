package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.Sku
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SkuDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<SkuDao>(Sku)
    var skuGroupId by Sku.skuGroupId
    var skuId by Sku.skuId
    var additionalFieldId by Sku.additionalFieldId
    var erpCode by Sku.erpCode
    var code by Sku.code
    var matCode by Sku.matCode
    var name by Sku.name
    var thickness by Sku.thickness
    var width by Sku.width
    var length by Sku.length
    var widthUom by Sku.widthUom
    var lengthUom by Sku.lengthUom
    var thicknessUom by Sku.thicknessUom
    var circumference by Sku.circumference
    var circumferenceUom by Sku.circumferenceUom
    var volumnM3 by Sku.volumnM3
    var volumnFt3 by Sku.volumnFt3
    var areaM2 by Sku.areaM2
    var species by Sku.species
    var grade by Sku.grade
    var fsc by Sku.fsc
    var extraAttributes by Sku.extraAttributes
    var createdAt by Sku.createdAt
    var updatedAt by Sku.updatedAt
    var status by Sku.status
}