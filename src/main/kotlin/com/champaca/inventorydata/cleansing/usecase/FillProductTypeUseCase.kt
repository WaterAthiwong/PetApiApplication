package com.champaca.inventorydata.cleansing.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.databasetable.GmItem
import com.champaca.inventorydata.databasetable.GoodMovement
import com.champaca.inventorydata.databasetable.Sku
import com.champaca.inventorydata.databasetable.dao.GoodMovementDao
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class FillProductTypeUseCase(
    val dataSource: DataSource
) {
    fun execute() {
        Database.connect(dataSource)

        transaction {
            addLogger(ExposedInfoLogger)
            val goodMovements = getGoodMovements()
            goodMovements.forEach { goodMovement ->
                val skuGroupId = findProductGroup(goodMovement.id.value)
                if (skuGroupId != -1) {
                    val type = when(skuGroupId) {
                        1 -> "ไม้ซุง"
                        2 -> "ไม้แปรรูป"
                        3 -> "ไม้ SLAB"
                        4 -> "ไม้แปรรูป"
                        5 -> "ไม้แปรรูป"
                        6 -> "ไม้แปรรูป"
                        7 -> "ไม้ SLAB"
                        8 -> "ไม้แปรรูป"
                        9 -> "ไม้แปรรูป"
                        10 -> "COMPONENT"
                        else -> "ไม้แปรรูป"
                    }

                    goodMovement.productType = type
                }
            }
        }
    }

    private fun getGoodMovements(): List<GoodMovementDao> {
        return GoodMovementDao.find { GoodMovement.id greaterEq  1500 }.toList()
    }

    private fun findProductGroup(goodMovementId: Int): Int {
        val joins = GmItem.join(GoodMovement, JoinType.INNER) { GmItem.goodMovementId eq GoodMovement.id }
            .join(Sku, JoinType.INNER) { Sku.id eq GmItem.skuId }
        val query = joins.select(Sku.skuGroupId)
            .where { (GoodMovement.id eq goodMovementId) and (GmItem.status eq "A")}
            .limit(1)
        return query.map { it[Sku.skuGroupId] }.firstOrNull() ?: -1
    }
}