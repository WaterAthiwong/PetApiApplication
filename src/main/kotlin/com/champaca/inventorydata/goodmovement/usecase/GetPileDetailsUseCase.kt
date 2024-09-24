package com.champaca.inventorydata.goodmovement.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.PileTransactionDao
import com.champaca.inventorydata.goodmovement.GoodMovementService
import com.champaca.inventorydata.goodmovement.response.GetPileDetailsResponse
import com.champaca.inventorydata.masterdata.user.UserRepository
import com.champaca.inventorydata.pile.PileService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class GetPileDetailsUseCase(
    val dataSource: DataSource,
    val userRepository: UserRepository,
    val pileService: PileService
) {

    companion object {
        const val PILE_EDIT_PERMISSION = "pile edit"
        const val PILE_REMOVE_PERMISSION = "pile delete"
        const val PILE_UNDO_PERMISSION = "pile undo"
    }

    val RECORED_DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    val logger = LoggerFactory.getLogger(GetPileDetailsUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(userId: String, goodMovementId: Int): GetPileDetailsResponse {
        Database.connect(dataSource)

        var result: GetPileDetailsResponse? = null
        transaction {
            addLogger(exposedLogger)
            val piles = getPiles(userId, goodMovementId)
            result = GetPileDetailsResponse(
                piles = piles,
                pileCount = piles.size,
                itemCount = piles.sumOf { pile -> pile.items.sumOf { item -> item.qty } },
                totalVolumnFt3 = piles.sumOf { it.totalVolumnFt3 }.setScale(2, RoundingMode.HALF_UP)
            )
        }
        return result!!
    }

    private fun getPiles(userId: String, goodMovementId: Int): List<GetPileDetailsResponse.PileData> {
        val joins = GmItem.join(GoodMovement, JoinType.INNER) {GmItem.goodMovementId eq GoodMovement.id }
            .join(Sku, JoinType.INNER) { Sku.id eq GmItem.skuId }
            .join(LotNo, JoinType.INNER) { LotNo.id eq GmItem.lotNoId }
            .join(PileHasLotNo, JoinType.INNER) { PileHasLotNo.lotNoId eq LotNo.id }
            .join(Pile, JoinType.INNER) { Pile.id eq PileHasLotNo.pileId }
            .join(StoreLocation, JoinType.INNER) { StoreLocation.id eq GmItem.storeLocationId }
            .join(StoreZone, JoinType.INNER) { StoreZone.id eq StoreLocation.storeZoneId }

        val query = joins.select(Pile.id, Pile.code, PileHasLotNo.lotSet, Pile.orderNo, Pile.remark, Pile.goodMovementId,
            Sku.matCode, GmItem.qty, Sku.volumnFt3, GmItem.createdAt, StoreLocation.code, Pile.originGoodMovementId,
            StoreZone.departmentId, GoodMovement.departmentId, GmItem.remark, GmItem.id, Sku.areaM2)
            .where{ (GmItem.goodMovementId eq goodMovementId) and (LotNo.status eq "A") and (Pile.status eq "A") and (GmItem.status eq "A") }

        val result = query.toList().groupBy( { it[Pile.code]}, { it })
            .mapValues { (_, rows) ->
                val firstRow = rows.first()
                val actionable = pileService.checkPileActionable(userId.toInt(), goodMovementId, firstRow)
                val isPartialPick = rows.any { it[GmItem.remark] != null && it[GmItem.remark]!!.contains(PileTransactionDao.PARTIAL_PICK) }
                GetPileDetailsResponse.PileData(
                    id = firstRow[Pile.id].value,
                    code = firstRow[Pile.code],
                    productGroup = firstRow[Sku.matCode].substring(1, 3),
                    orderNo = firstRow[Pile.orderNo],
                    remark = firstRow[Pile.remark],
                    canUndo = if (isPartialPick) true else actionable.canUndo,
                    canEdit = if (isPartialPick) false else actionable.canEdit,
                    canRemove = if (isPartialPick) false else actionable.canRemove,
                    lotSet = firstRow[PileHasLotNo.lotSet],
                    totalQty = rows.map { it[GmItem.qty] }.reduce(BigDecimal::add),
                    totalVolumnFt3 = rows.map { it[Sku.volumnFt3].multiply(it[GmItem.qty]) }.reduce(BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP),
                    totalAreaM2 = rows.map {
                            val areaM2 = it[Sku.areaM2]
                            val qty = it[GmItem.qty]
                            if (areaM2 != null && qty != null) {
                                areaM2.multiply(qty)
                            } else {
                                BigDecimal.ZERO // Or handle null case appropriately
                            }
                        }.reduce(BigDecimal::add).setScale(2, RoundingMode.HALF_UP),
                    location = firstRow[StoreLocation.code],
                    isPartialPick = isPartialPick,
                    items = rows.map {
                        GetPileDetailsResponse.Item(
                            matCode = it[Sku.matCode],
                            qty = it[GmItem.qty],
                            volumnFt3 = it[Sku.volumnFt3].multiply(it[GmItem.qty]).setScale(2, RoundingMode.HALF_UP),
                            areaM2 = it[Sku.areaM2]?.multiply(it[GmItem.qty])?.setScale(2, RoundingMode.HALF_UP),
                            recordedAt = RECORED_DATETIME_FORMAT.format(it[GmItem.createdAt]),
                            location = it[StoreLocation.code],
                            gmItemId = it[GmItem.id].value
                        )
                    }
                )
            }
        return result.values.toList()
    }

    private fun checkPileActionPermission(userId: Int, goodMovementId: Int, row: ResultRow): Triple<Boolean, Boolean, Boolean> {
        val permissions = userRepository.getUserPermissions(userId)
        val canEdit = permissions.contains(PILE_EDIT_PERMISSION) && row[Pile.goodMovementId].value == row[Pile.originGoodMovementId] &&
                row[StoreZone.departmentId] == row[GoodMovement.departmentId].value
        val canRemove = permissions.contains(PILE_REMOVE_PERMISSION) && row[Pile.goodMovementId].value == row[Pile.originGoodMovementId] &&
                row[StoreZone.departmentId] == row[GoodMovement.departmentId].value
        val canUndo = permissions.contains(PILE_UNDO_PERMISSION) && row[Pile.goodMovementId].value == goodMovementId
        return Triple(canEdit, canRemove, canUndo)
    }
}