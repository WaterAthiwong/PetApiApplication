package com.champaca.inventorydata.masterdata.supplier

import com.champaca.inventorydata.databasetable.Supplier
import com.champaca.inventorydata.databasetable.dao.SupplierDao
import org.jetbrains.exposed.sql.and
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class SupplierRepository {
    fun getAll(): List<SupplierDao> {
        return SupplierDao.find { Supplier.status eq "A" }.toList()
    }

    @Cacheable("suppliers")
    fun findById(id: Int): SupplierDao? {
        return SupplierDao.findById(id)
    }

    @Cacheable("suppliersByType")
    fun findByType(types: List<String>): List<SupplierDao> {
        return SupplierDao.find { (Supplier.status eq "A") and (Supplier.type inList types) }.toList()
    }
}