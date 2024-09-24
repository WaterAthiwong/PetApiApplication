package com.champaca.inventorydata.databasetable.dao

import com.champaca.inventorydata.databasetable.Supplier
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SupplierDao (id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<SupplierDao>(Supplier) {
        const val INTERNAL = "internal" // หน่วยงานภายในจัมปาก้าที่มีการทำใบเบิกทั่วไป เช่น แผนกไม้พื้น
        const val FORESTRY = "forestry" // สวนป่าที่ส่งไม้ซุงให้จัมปาก้า
        const val SUPPLIER = "supplier" // ซัพพลายเออร์อื่นๆ ที่ไม่ใช่สวนป่า
        const val CUSTOMER = "customer" // ลูกค้าที่จัมปาก้าส่งสินค้าให้
        const val SUPCUST = "supCust" // คนที่เป็นทั้งลูกค้าและ Supplier
    }

    var name by Supplier.name
    var type by Supplier.type
    var taxNo by Supplier.taxNo
    var address by Supplier.address
    var phone by Supplier.phone
    var email by Supplier.email
    var contact by Supplier.contact
    var erpCode by Supplier.erpCode
    var remark by Supplier.remark
    var status by Supplier.status
}