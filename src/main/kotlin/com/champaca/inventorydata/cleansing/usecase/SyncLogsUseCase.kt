package com.champaca.inventorydata.cleansing.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.LotNoDao
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.pile.usecase.PickPileUseCase
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Deprecated("Use for syncing log data only")
@Service
class SyncLogsUseCase(
    val dataSource: DataSource,
    val wmsService: WmsService,
    val pickPileUseCase: PickPileUseCase
) {

    val pickGoodMovementId = 7363

    fun execute(sessionId: String, barcodes: List<String>) {
        Database.connect(dataSource)

        var itemMovements = listOf<WmsService.ItemMovementEntry>()
//        transaction {
//            addLogger(ExposedInfoLogger)
//            val logsMap = findAllLogsInLogYard().associateBy { it.refCode }
//            val presentInLogYardLogs = logsMap.keys.toList()
//            val logsFromNamList = findFromNamList(barcodes)
//            val hasBeenPickedLogs = presentInLogYardLogs.filter { !logsFromNamList.contains(it) }
//            val logs = findAllLogsInLogYard()
//            itemMovements = createItemMovementsFromLotNo(logsFromNamList.map { it.id.value })
//            println("Has been Picked: ${logs.map { "\"${it.refCode}\"" }.joinToString(", ")}")
//        }

        var nonExistingPiles = listOf<String>()
        transaction {
            addLogger(ExposedInfoLogger)
            val allLots = findAllItemsInStorage()
            val lotsInPiles = findItemsInStorageThatInPile()
//            val piles = findPileFromLotNo(lotsInPiles.map { it.id.value })
            val nonExistingLots = allLots.filter { !lotsInPiles.contains(it) }
            itemMovements = createItemMovementsFromLotNo(nonExistingLots.map { it.id.value })
//            println("NonExistingPile: ${nonExistingLots.map { it.id.value }.joinToString(", ")}")
        }

//        for (pile in nonExistingPiles) {
//            val request = PickPileRequest(pileCode = pile, goodMovementId = pickGoodMovementId, manufacturingLineId = null)
//            pickForProcessUseCase.execute(sessionId, "10", request)
//        }

        var index = 0
        while (index < itemMovements.size) {
            val end = minOf(index + 299, itemMovements.size - 1)
            val batch = itemMovements.slice(index..end)
            println("About to pick items ${index} til ${end}")
            wmsService.pickGmItem(sessionId, batch)
            index += 300
        }
    }

    private fun findFromNamList(barcodes: List<String>): List<String> {
        val joins = StoreLocationHasLotNo.join(LotNo, JoinType.INNER) { StoreLocationHasLotNo.lotNoId eq LotNo.id }
            .join(Sku, JoinType.INNER) { StoreLocationHasLotNo.skuId eq Sku.id }
        val query = joins.select(LotNo.refCode)
            .where { (LotNo.refCode.like("A%")) and (Sku.status eq "A")  and (Sku.skuGroupId eq 1) and (LotNo.status eq "A") and (LotNo.refCode.inList(barcodes)) }
        return query.map { it[LotNo.refCode] }
    }

    private fun findAllLogsInLogYard(): List<LotNoDao> {
        val joins = StoreLocationHasLotNo.join(LotNo, JoinType.INNER) { StoreLocationHasLotNo.lotNoId eq LotNo.id }
            .join(Sku, JoinType.INNER) { StoreLocationHasLotNo.skuId eq Sku.id }
        val query = joins.select(LotNo.columns)
            .where { (LotNo.refCode.like("Z%")) and (Sku.status eq "A")  and (Sku.skuGroupId eq 1) and (LotNo.status eq "A") }
        return query.map { LotNoDao.wrapRow(it) }
    }

    private fun createItemMovementsFromLog(barcodes: List<String>): List<WmsService.ItemMovementEntry> {
        val joins = LotNo.join(StoreLocationHasLotNo, JoinType.INNER) { LotNo.id eq StoreLocationHasLotNo.lotNoId }
            .join(Sku, JoinType.INNER) { StoreLocationHasLotNo.skuId eq Sku.id }
            .join(StoreLocation, JoinType.INNER) { StoreLocationHasLotNo.storeLocationId eq StoreLocation.id }
        val query = joins.select(Sku.id, StoreLocation.id, StoreLocation.code, LotNo.code, LotNo.id, LotNo.refCode)
            .where { (LotNo.refCode.inList(barcodes)) and (LotNo.status eq "A") }
        return query.map {
            WmsService.ItemMovementEntry(
                goodMovementType = GoodMovementType.PICKING_ORDER,
                goodMovementId = pickGoodMovementId,
                skuId = it[Sku.id].value,
                sku = null,
                storeLocationId = it[StoreLocation.id].value,
                storeLocation = it[StoreLocation.code],
                manufacturingLineId = null,
                lotNo = it[LotNo.code],
                lotNoId = it[LotNo.id].value,
                qty = 1.toBigDecimal(),
                refCode = it[LotNo.refCode],
                remark = "Data Cleansing"
            )
        }
    }

    private fun findAllItemsInStorage(): List<LotNoDao> {
        val joins = StoreLocationHasLotNo.join(StoreLocation, JoinType.INNER) { StoreLocationHasLotNo.storeLocationId eq StoreLocation.id }
            .join(LotNo, JoinType.INNER) { StoreLocationHasLotNo.lotNoId eq LotNo.id }
        val query = joins.select(LotNo.columns)
            .where { (StoreLocation.code.like("BPKD%")) and (LotNo.status eq "A") }
        return query.map { LotNoDao.wrapRow(it) }
    }

    private fun findItemsInStorageThatInPile(): List<LotNoDao> {
        val joins = StoreLocationHasLotNo.join(StoreLocation, JoinType.INNER) { StoreLocationHasLotNo.storeLocationId eq StoreLocation.id }
            .join(LotNo, JoinType.INNER) { StoreLocationHasLotNo.lotNoId eq LotNo.id }
            .join(PileHasLotNo, JoinType.INNER) { PileHasLotNo.lotNoId eq LotNo.id }
        val query = joins.select(LotNo.columns)
            .where { (StoreLocation.code.like("BPKD%")) and (LotNo.status eq "A") }
        return query.map { LotNoDao.wrapRow(it) }
    }

    private fun findPileFromLotNo(lotNoIds: List<Int>): List<String> {
        val joins = PileHasLotNo.join(Pile, JoinType.INNER) { PileHasLotNo.pileId eq Pile.id }
        val query = joins.select(Pile.code)
            .where { (PileHasLotNo.lotNoId.inList(lotNoIds)) and (Pile.status eq "A") and (Pile.createdAt.date() lessEq  stringLiteral("2024-02-16")) }

        return query.distinct().map { it[Pile.code] }
    }

    private fun createItemMovementsFromLotNo(lotNoIds: List<Int>): List<WmsService.ItemMovementEntry> {
        val joins = LotNo.join(StoreLocationHasLotNo, JoinType.INNER) { LotNo.id eq StoreLocationHasLotNo.lotNoId }
            .join(Sku, JoinType.INNER) { StoreLocationHasLotNo.skuId eq Sku.id }
            .join(StoreLocation, JoinType.INNER) { StoreLocationHasLotNo.storeLocationId eq StoreLocation.id }
        val query = joins.select(Sku.id, StoreLocation.id, StoreLocation.code, LotNo.code, LotNo.id, LotNo.refCode, StoreLocationHasLotNo.qty)
            .where { (LotNo.id.inList(lotNoIds)) and (LotNo.status eq "A") }
        return query.map {
            WmsService.ItemMovementEntry(
                goodMovementType = GoodMovementType.PICKING_ORDER,
                goodMovementId = pickGoodMovementId,
                skuId = it[Sku.id].value,
                sku = null,
                storeLocationId = it[StoreLocation.id].value,
                storeLocation = it[StoreLocation.code],
                manufacturingLineId = null,
                lotNo = it[LotNo.code],
                lotNoId = it[LotNo.id].value,
                qty = it[StoreLocationHasLotNo.qty],
                refCode = it[LotNo.refCode],
                remark = "Data Cleansing"
            )
        }
    }
}