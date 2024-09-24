package com.champaca.inventorydata.cleansing.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class CheckLotStockUseCase(
    val dataSource: DataSource
) {
    fun execute(lotNoIds: List<Int>) {
        Database.connect(dataSource)

        transaction {
            addLogger(ExposedInfoLogger)
            val receipts = getReceiptNumber(lotNoIds)

            val lotToPileMap = receipts.associateBy { it[LotNo.id].value }.mapValues { it.value[Pile.code] }
            val lotToMatCodeMap = receipts.associateBy { it[LotNo.id].value }.mapValues { it.value[Sku.matCode] }
            val lotToReceivedQtyMap = receipts.associateBy { it[LotNo.id].value }.mapValues { it.value[GmItem.qty] }

            val picks = getPickingNumber(lotNoIds)
            val lotToPickedQtyMap = picks.groupBy { it[LotNo.id].value }.mapValues { entry -> entry.value.sumOf { it[GmItem.qty] } }

            val matCodeToRemainingQtyMap = mutableMapOf<String, BigDecimal>()
            lotToReceivedQtyMap.forEach { (lotNoId, receivedQty) ->
                val pickedQty = lotToPickedQtyMap[lotNoId] ?: BigDecimal.ZERO
                val remainingQty = receivedQty - pickedQty
                val pileCode = lotToPileMap[lotNoId]
                val matCode = lotToMatCodeMap[lotNoId]!!
                println("$pileCode, $matCode, $remainingQty")

                if (remainingQty > BigDecimal.ZERO) {
                    val currentRemainingQty = matCodeToRemainingQtyMap[matCode] ?: BigDecimal.ZERO
                    matCodeToRemainingQtyMap[matCode] = currentRemainingQty + remainingQty
                }
            }

            println()
            println()

            matCodeToRemainingQtyMap.forEach { (matCode, remainingQty) ->
                println("$matCode, $remainingQty")
            }
        }
    }

    private fun getReceiptNumber(lotNoIds: List<Int>): List<ResultRow> {
        val joins = GmItem.join(LotNo, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(GoodMovement, JoinType.INNER) { (GoodMovement.id eq GmItem.goodMovementId) and (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) }
            .join(Sku, JoinType.INNER) { Sku.id eq GmItem.skuId }
            .join(PileHasLotNo, JoinType.LEFT) { PileHasLotNo.lotNoId eq LotNo.id }
            .join(Pile, JoinType.LEFT) { (Pile.id eq PileHasLotNo.pileId) }
        val query = joins.select(LotNo.id, GmItem.qty, Sku.matCode, Pile.code)
            .where { (LotNo.id inList lotNoIds) and (GmItem.status eq "A") and (LotNo.status eq "A") }
        return query.toList()
    }

    private fun getPickingNumber(lotNoIds: List<Int>): List<ResultRow> {
        val joins = GmItem.join(LotNo, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(GoodMovement, JoinType.INNER) { (GoodMovement.id eq GmItem.goodMovementId) and (GoodMovement.type eq GoodMovementType.PICKING_ORDER.wmsName) }
        val query = joins.select(LotNo.id, GmItem.qty)
            .where { (LotNo.id inList lotNoIds) and (GmItem.status eq "A") and (LotNo.status eq "A") and
                    (GmItem.createdAt lessEq LocalDateTime.parse("2024-05-31T23:59:59")) }
        return query.toList()
    }
}