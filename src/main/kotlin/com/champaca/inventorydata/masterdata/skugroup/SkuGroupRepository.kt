package com.champaca.inventorydata.masterdata.skugroup

import com.champaca.inventorydata.databasetable.SkuGroup
import com.champaca.inventorydata.databasetable.dao.SkuGroupDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class SkuGroupRepository {

    @Cacheable("skuGroups")
    fun getAll(): List<SkuGroupDao> {
        return SkuGroupDao.find { SkuGroup.status eq "A" }.toList()
    }
}