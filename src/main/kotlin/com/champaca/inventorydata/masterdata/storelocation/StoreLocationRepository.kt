package com.champaca.inventorydata.masterdata.storelocation

import com.champaca.inventorydata.databasetable.StoreLocation
import com.champaca.inventorydata.databasetable.StoreZone
import com.champaca.inventorydata.databasetable.dao.StoreLocationDao
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class StoreLocationRepository {

    @Cacheable("storeLocationIds")
    fun getById(id: Int): StoreLocationDao? {
        return StoreLocationDao.findById(id)
    }

    @Cacheable("storeLocationCodes")
    fun getByCode(code: String): StoreLocationDao? {
        return StoreLocationDao.find {  (StoreLocation.code eq code) and (StoreLocation.status eq "A") }.singleOrNull()
    }

    @Cacheable("storeLocationDepartmentIds")
    fun getDepartmentId(id: Int): Int {
        val joins = StoreLocation.join(StoreZone, JoinType.INNER) { StoreLocation.storeZoneId eq StoreZone.id }
        val query = joins.select(StoreZone.departmentId)
            .where { (StoreLocation.id eq id) and (StoreLocation.status eq "A") }
        return query.single()[StoreZone.departmentId]
    }
}