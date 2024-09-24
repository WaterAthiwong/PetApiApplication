package com.champaca.inventorydata.masterdata.sku

import com.champaca.inventorydata.databasetable.Sku
import com.champaca.inventorydata.databasetable.SkuGroup
import com.champaca.inventorydata.databasetable.dao.SkuDao
import org.jetbrains.exposed.sql.*
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class SkuRepository(

) {
    fun findNonExistingMatCodes(matCodes: List<String>): List<String> {
        val existingMatCodes = Sku.select(Sku.matCode)
            .where { (Sku.matCode.inList(matCodes)) and (Sku.status eq "A") }
            .map { resultRow -> resultRow[Sku.matCode] }

        if(matCodes.size == existingMatCodes.size) {
           return listOf()
        }
        return matCodes.filter { !existingMatCodes.contains(it) }
    }

    fun deleteDupe(): List<Pair<String, Int>> {
        val maxAlias = Sku.id.max().alias("greater_id")
        val query = Sku.select(Sku.matCode, maxAlias)
            .where { Sku.status eq "A" }
            .groupBy(Sku.matCode)
            .orderBy(maxAlias to SortOrder.DESC)
            .having { Sku.id.count() greaterEq 2 }

        val matcodes: List<Pair<String, Int>> = query.map { resultRow -> Pair(resultRow[Sku.matCode], resultRow[maxAlias]!!.value) }
        val ids = matcodes.map { pair -> pair.second }

        Sku.update({Sku.id.inList(ids)}) {
            it[Sku.status] = "D"
        }
        return matcodes
    }

    fun getSkuIdsFromMatCodes(matCodes: List<String>): Map<String, Int> {
        val query = Sku.select(Sku.id, Sku.matCode).where { (Sku.matCode.inList(matCodes)) and (Sku.status eq "A") }
        return query.map { resultRow -> Pair(resultRow[Sku.id].value, resultRow[Sku.matCode]) }.associateBy({it.second}, {it.first})
    }

    @Cacheable("skusByMatCode")
    fun findByMatCode(matCode: String): SkuDao? {
        return SkuDao.find { (Sku.status eq "A") and (Sku.matCode eq matCode) }.singleOrNull()
    }

    @Cacheable("skusByGroupCode")
    fun findBySkuGroupCode(skuGroup: String): List<SkuDao> {
        val joins = Sku.join(SkuGroup, JoinType.INNER) { Sku.skuGroupId eq SkuGroup.id }
        return joins.select(Sku.columns).where { (SkuGroup.erpGroupCode eq skuGroup) and (Sku.status eq "A") }.map { SkuDao.wrapRow(it) }
    }
}