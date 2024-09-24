package com.champaca.inventorydata.masterdata.manufacturingline

import com.champaca.inventorydata.databasetable.ManufacturingLine
import com.champaca.inventorydata.databasetable.ProcessType
import com.champaca.inventorydata.databasetable.dao.ManufacturingLineDao
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class ManufacturingLineRepository {

    @Cacheable("manufacturingLines")
    fun findById(id: Int?): ManufacturingLineDao? {
        if (id == null) return null
        return ManufacturingLineDao.findById(id)
    }

    fun getAll(): List<ManufacturingLineDao> {
        return ManufacturingLineDao.all().toList()
    }

    @Cacheable("processAndManufacturingLines")
    fun getProcessAndManufacturingLine(departmentId: Int): List<ResultRow> {
        val joins = ProcessType.join(ManufacturingLine, JoinType.INNER) { ProcessType.id eq ManufacturingLine.processTypeId }
        val query = joins.select(
            ProcessType.id, ProcessType.name, ProcessType.prefix,
            ManufacturingLine.id, ManufacturingLine.name)
            .where { ProcessType.departmentId eq departmentId }
        return query.toList()
    }
}