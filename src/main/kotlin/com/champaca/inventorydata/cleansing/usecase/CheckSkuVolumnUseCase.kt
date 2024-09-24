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
class CheckSkuVolumnUseCase(
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
                val volumnFt3 = when {
                    sku.thicknessUom == "mm" && sku.widthUom == "mm" && sku.lengthUom == "mm" ->
                        (sku.thickness * sku.width * sku.length * M3_TO_FT3).divide(1000000000.toBigDecimal(), 5, RoundingMode.HALF_UP)
                    sku.thicknessUom == "in" && sku.widthUom == "in" && sku.lengthUom == "ft" ->
                        (sku.thickness * sku.width * sku.length).divide(144.toBigDecimal(), 5, RoundingMode.HALF_UP)
                    sku.thicknessUom == "mm" && sku.widthUom == "mm" && sku.lengthUom == "m" ->
                        (sku.thickness * sku.width * sku.length * M3_TO_FT3).divide(1000000.toBigDecimal(), 5, RoundingMode.HALF_UP)
                    sku.thicknessUom == "in" && sku.widthUom == "in" && sku.lengthUom == "m" ->
                        (sku.thickness * sku.width * sku.length * M_TO_FT).divide(144.toBigDecimal(), 5, RoundingMode.HALF_UP)
                    else -> {
                        wrongUoms.add(sku.matCode)
                        BigDecimal.ZERO
                    }
                }

                val volumnM3 = volumnFt3.divide(M3_TO_FT3, 5, RoundingMode.HALF_UP)

                if ((volumnM3 - sku.volumnM3).abs() > BigDecimal("0.00004")) {
                    results.add("SKU ${sku.id}, ${sku.matCode} has different volumnM3: ${sku.volumnM3} vs $volumnM3")
                    troubles.add(sku.id.value)
                }

                if ((volumnM3 - sku.volumnM3).abs() > BigDecimal("0.01000") && !wideGaps.contains(sku.matCode)) {
                    wideGaps.add("${sku.matCode} M3 ใน DB: ${sku.volumnM3} ที่คำนวณใหม่: $volumnM3")
                }

                if ((volumnFt3 - sku.volumnFt3).abs() > BigDecimal("0.00004")) {
                    results.add("SKU ${sku.id}, ${sku.matCode} has different volumnFt3: ${sku.volumnFt3} vs $volumnFt3")
                    troubles.add(sku.id.value)
                }
                if ((volumnFt3 - sku.volumnFt3).abs() > BigDecimal("0.01000") && !wideGaps.contains(sku.matCode)) {
                    wideGaps.add("${sku.matCode} FT3 ใน DB: ${sku.volumnFt3} ที่คำนวณใหม่: $volumnFt3")
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
            .where { (Sku.status eq "A") and (SkuGroup.erpMainGroupId neq 5) and (SkuGroup.id notInList listOf(1, 114))}
        return SkuDao.wrapRows(query).toList()
    }

    private fun findExistingSku(skuIds: List<Int>): List<Int> {
        val query = GmItem.select(GmItem.skuId).where { GmItem.skuId inList skuIds }.withDistinct()
        return query.map { it[GmItem.skuId] }
    }

}