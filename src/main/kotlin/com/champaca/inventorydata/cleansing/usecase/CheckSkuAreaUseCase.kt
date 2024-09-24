package com.champaca.inventorydata.cleansing.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.common.ChampacaConstant.M3_TO_FT3
import com.champaca.inventorydata.databasetable.GmItem
import com.champaca.inventorydata.databasetable.Sku
import com.champaca.inventorydata.databasetable.SkuGroup
import com.champaca.inventorydata.databasetable.dao.SkuDao
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import javax.sql.DataSource

@Service
class CheckSkuAreaUseCase(
    val dataSource: DataSource
) {
    companion object {
        val M_TO_FT = 3.28084.toBigDecimal()
    }

    fun execute(): List<String> {
        Database.connect(dataSource)

        var results = mutableListOf<String>()
        transaction {
            addLogger(ExposedInfoLogger)
            val skus = getSkus()
            val troubles = mutableSetOf<Int>()
            val wrongUoms = mutableSetOf<String>()
            val wideGaps = mutableListOf<String>()

            skus.forEach { sku ->
                val areaM2 = when {
                    sku.widthUom == "mm" && sku.lengthUom == "mm" ->
                        (sku.width * sku.length).divide(1000000.toBigDecimal(), 5, RoundingMode.HALF_UP)
                    sku.widthUom == "in" && sku.lengthUom == "ft" ->
                        (sku.width * sku.length * 12.toBigDecimal() * 0.00064516.toBigDecimal()).setScale(5, RoundingMode.HALF_UP)
                    else -> {
                        wrongUoms.add(sku.matCode)
                        BigDecimal.ZERO
                    }
                }

                if ((areaM2 - sku.areaM2!!).abs() > BigDecimal("0.1")) {
                    results.add("SKU ${sku.id}, ${sku.matCode} has different areaM2: ${sku.areaM2} vs $areaM2")
                    wideGaps.add(sku.matCode)
                }
            }

//            val existing = findExistingSku(troubles.toList())
//            if (existing.isNotEmpty()) {
//                results.add("Existing SKU: ${existing.joinToString(", ")}")
//            }
            results.add("Wrong UOMs: ${wrongUoms.joinToString(", ")}")
            results.add("Wide gaps: ${wideGaps.joinToString(", ")}")
        }

        return results
    }

    private fun getSkus(): List<SkuDao> {
        val joins = Sku.join(SkuGroup, JoinType.INNER) { Sku.skuGroupId eq SkuGroup.id }
        val query = joins.select(Sku.columns)
            .where { (Sku.status eq "A") and (SkuGroup.id notInList listOf(1, 114)) and (Sku.areaM2.isNotNull()) }
        return SkuDao.wrapRows(query).toList()
    }

    private fun findExistingSku(skuIds: List<Int>): List<Int> {
        val query = GmItem.select(GmItem.skuId).where { GmItem.skuId inList skuIds }.withDistinct()
        return query.map { it[GmItem.skuId] }
    }

}