package com.champaca.inventorydata.conditionroom

import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.conditionroom.model.SuggestedShelf
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.ConditionRoomTransferDao
import com.champaca.inventorydata.databasetable.dao.LotNoDao
import com.champaca.inventorydata.masterdata.config.ConfigRepository
import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.request.PileItem
import com.champaca.inventorydata.utils.DateTimeUtil
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.sum
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class ConditionRoomService(
    val dateTimeUtil: DateTimeUtil,
    val configRepository: ConfigRepository
) {

    companion object {
        const val CONDITION_ROOM_DEPARTMENT_ID = 6
    }

    fun getRemainingItems(goodMovementId: Int): List<MovingItem> {
        return getRemainingItems(listOf(goodMovementId))
    }

    fun getRemainingItems(goodMovementIds: List<Int>): List<MovingItem> {
        val initJoins = GmItem.join(LotNo, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(Sku, JoinType.INNER) { GmItem.skuId eq Sku.id }
            .join(PileHasLotNo, JoinType.INNER) { PileHasLotNo.lotNoId eq LotNo.id }
            .join(Pile, JoinType.INNER) { (Pile.id eq PileHasLotNo.pileId) and (Pile.lotSet eq PileHasLotNo.lotSet) }
        val initRows = initJoins.select(GmItem.skuId, GmItem.qty, LotNo.refCode, LotNo.id, Sku.matCode, GmItem.id,
                Pile.code, Sku.thickness, Sku.width, Sku.length, Sku.grade, Sku.fsc, Sku.species, Sku.volumnFt3,
                Sku.volumnM3, Sku.areaM2)
            .where { (GmItem.goodMovementId inList goodMovementIds) and
                    (GmItem.status eq "A") and (LotNo.status eq "A") }
            .toList()

        val remainingItems = initRows.map { row ->
            MovingItem(
                lotNoId = row[LotNo.id].value,
                lotCode = "",
                lotRefCode = row[LotNo.refCode],
                skuId = row[GmItem.skuId],
                matCode = row[Sku.matCode],
                skuName = "",
                width = row[Sku.width],
                widthUom = "",
                length = row[Sku.length],
                lengthUom = "",
                thickness = row[Sku.thickness],
                thicknessUom = "",
                volumnFt3 = row[Sku.volumnFt3],
                volumnM3 = row[Sku.volumnM3],
                grade = row[Sku.grade],
                fsc = row[Sku.fsc],
                species = row[Sku.species],
                skuGroupId = -1,
                storeLocationId = -1,
                storeLocationCode = "",
                qty = row[GmItem.qty]
            ).apply {
                gmItemId = row[GmItem.id].value
                pilecode = row[Pile.code]
            }
        }

        val remainingItemsByLotNo = remainingItems.associateBy { it.lotNoId }
        val conditionTransfers = ConditionRoomTransferDao.find { ConditionRoomTransfer.transferGoodMovementId inList goodMovementIds }
            .toList()
        conditionTransfers.forEach { row ->
            val lotNoId = row.transferredLotNoId
            val remainingItem = remainingItemsByLotNo[lotNoId] ?: return@forEach
            remainingItem.qty -= row.qty
        }

        return remainingItems.filter { it.qty > BigDecimal.ZERO }
    }

    fun getSuggestedShelves(items: List<MovingItem>): List<SuggestedShelf> {
        val skuIds = items.map { it.skuId }.distinct()
        val joins = StoreLocationHasLotNo.join(LotNo, JoinType.INNER) { StoreLocationHasLotNo.lotNoId eq LotNo.id }
            .join(Sku, JoinType.INNER) { StoreLocationHasLotNo.skuId eq Sku.id }
            .join(PileHasLotNo, JoinType.INNER) { PileHasLotNo.lotNoId eq LotNo.id }
            .join(Pile, JoinType.INNER) { Pile.id eq PileHasLotNo.pileId }
            .join(StoreLocation, JoinType.INNER) { StoreLocation.id eq StoreLocationHasLotNo.storeLocationId }
            .join(StoreZone, JoinType.INNER) { StoreZone.id eq StoreLocation.storeZoneId }
        val query = joins.select(Sku.matCode, Sku.thickness, Sku.width, Sku.length, Sku.grade, Pile.code, StoreLocation.code, StoreLocationHasLotNo.qty.sum())
            .where { (StoreLocationHasLotNo.skuId inList skuIds) and (LotNo.status eq "A") and
                    (Pile.status eq "A") and (StoreZone.departmentId eq CONDITION_ROOM_DEPARTMENT_ID) }
            .groupBy(Sku.id, Pile.id)
        return query.map {
            SuggestedShelf(
                shelfCode = it[Pile.code],
                location = it[StoreLocation.code],
                matCode = it[Sku.matCode],
                qty = it[StoreLocationHasLotNo.qty.sum()]!!,
                thickness = it[Sku.thickness],
                width = it[Sku.width],
                length = it[Sku.length],
                grade = it[Sku.grade] ?: ""
            )
        }
    }

    fun verifyTransferInAmount(requestedItems: List<PileItem>, remainingItems: List<MovingItem>): ResultOf<Boolean> {
        // Verify ว่าทุก matcode ใน request มีจำนวนพอใน good movement ถ้าเจอว่าไม่พอให้ส่งเป็น ResultOf.Failure กลับไป
        val qtyBySkuId = remainingItems.groupBy { it.skuId }.mapValues { it.value.sumOf { it.qty } }
        val exceedingItems = mutableListOf<String>()
        requestedItems.forEach { item ->
            val qty = qtyBySkuId[item.skuId] ?: BigDecimal.ZERO
            if (item.qty > qty) {
                exceedingItems.add("${item.matCode}: ${item.qty - qty}")
            }
        }
        if (exceedingItems.isNotEmpty()) {
            return ResultOf.Failure(exceedingItems.joinToString(",\n"))
        }
        return ResultOf.Success(true)
    }

    fun deductAndTransfer(requestedItems: List<PileItem>, remainingItems: List<MovingItem>):
            Triple<List<MovingItem>, List<MovingItem>, List<MovingItem>>{
        val remainingItemsBySkuId = remainingItems.groupBy { it.skuId }
        val transferringItems = mutableListOf<MovingItem>()
        val deductedItems = mutableListOf<MovingItem>()
        requestedItems.forEach { item ->
            val rows = remainingItemsBySkuId[item.skuId]!!
            var neededQty = item.qty
            var index = 0
            while(neededQty > BigDecimal.ZERO && index < rows.size) {
                val row = rows[index]
                if (row.qty <= neededQty) {
                    transferringItems.add(row.copy(qty = row.qty).apply { pilecode = row.pilecode; gmItemId = row.gmItemId})
                    neededQty -= row.qty
                    row.qty = BigDecimal.ZERO
                } else {
                    transferringItems.add(row.copy(qty = neededQty).apply { pilecode = row.pilecode; gmItemId = row.gmItemId})
                    row.qty -= neededQty
                    neededQty = BigDecimal.ZERO
                }
                deductedItems.add(row.copy().apply { pilecode = row.pilecode; gmItemId = row.gmItemId})
                index++
            }
        }

        return Triple(remainingItems, transferringItems, deductedItems)
    }

    fun fillReceivingLotNoIds(receivingItems: List<WmsService.ItemMovementEntry>): List<WmsService.ItemMovementEntry> {
        val refCodes = receivingItems.map { it.refCode!! }
        val lotNoDaos = LotNoDao.find { (LotNo.refCode inList refCodes) and (LotNo.status eq "A") }
            .toList()
            .associateBy { it.refCode }
        receivingItems.forEach { item ->
            val lotNoDao = lotNoDaos[item.refCode!!]!!
            item.apply {
                lotNo = lotNoDao.code
                lotNoId = lotNoDao.id.value
            }
        }
        return receivingItems
    }

    fun getIntermediateGoodMovementId(prefix: String): ResultOf<Int> {
        val monthPrefix = dateTimeUtil.getYearMonthPrefix()
        val config = configRepository.get("$prefix$monthPrefix") ?: return ResultOf.Failure("$prefix$monthPrefix")
        return ResultOf.Success(config.valueInt!!)
    }

}