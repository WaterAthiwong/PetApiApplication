package com.champaca.inventorydata.cleansing.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Deprecated("Use for data cleansing only")
@Service
class RemoveNoTagUseCase(
    val dataSource: DataSource,
    val wmsService: WmsService
) {

    companion object {
        const val LOCATION_CODE = "BSCO%"
        const val PICKING_GOOD_MOVEMENT_ID = 9065
    }

    fun peak(): List<Int> {
        Database.connect(dataSource)

        var results = listOf<Int>()
        transaction {
            addLogger(ExposedInfoLogger)

            val lotNoIds = findAllLotNoIdsInLocation()
//            val inPiles = findLotNoIdsInPile(lotNoIds)
            val inPiles = emptyList<Int>()
            results = lotNoIds - inPiles
            println("Count: ${results.size}")
        }
        return results
    }

    fun execute(sessionId: String): Int {
        Database.connect(dataSource)

        var itemMovements = listOf<WmsService.ItemMovementEntry>()
        transaction {
            addLogger(ExposedInfoLogger)

            val lotNoIds = peak()
            itemMovements = findItemMovements(lotNoIds)
        }

        wmsService.pickGmItem(sessionId, itemMovements)
        return itemMovements.size
    }

    private fun findAllLotNoIdsInLocation(): List<Int> {
        val joins = StoreLocationHasLotNo.join(StoreLocation, JoinType.INNER) { StoreLocationHasLotNo.storeLocationId eq StoreLocation.id }
            .join(Sku, JoinType.INNER) { StoreLocationHasLotNo.skuId eq Sku.id }
        val query = joins.select(StoreLocationHasLotNo.lotNoId)
            .where { StoreLocation.code.like(LOCATION_CODE) }
        return query.map { it[StoreLocationHasLotNo.lotNoId].value }
    }

    private fun findLotNoIdsInPile(lotNoIds: List<Int>): List<Int> {
        val query = PileHasLotNo.select(PileHasLotNo.lotNoId).where(PileHasLotNo.lotNoId inList lotNoIds)
        return query.map { it[PileHasLotNo.lotNoId].value }
    }

    private fun findItemMovements(lotNoIds: List<Int>): List<WmsService.ItemMovementEntry> {
        val joins = StoreLocationHasLotNo.join(StoreLocation, JoinType.INNER) { StoreLocationHasLotNo.storeLocationId eq StoreLocation.id }
            .join(Sku, JoinType.INNER) { StoreLocationHasLotNo.skuId eq Sku.id }
            .join(LotNo, JoinType.INNER) { StoreLocationHasLotNo.lotNoId eq LotNo.id }
        val query = joins.select(StoreLocationHasLotNo.qty, StoreLocation.id, StoreLocation.code, Sku.id,
            Sku.matCode, LotNo.id, LotNo.code, LotNo.refCode)
            .where { LotNo.id inList lotNoIds }
        return query.map {
            WmsService.ItemMovementEntry(
                goodMovementType = GoodMovementType.PICKING_ORDER,
                goodMovementId = PICKING_GOOD_MOVEMENT_ID,
                skuId = it[Sku.id].value,
                sku = it[Sku.matCode],
                storeLocationId = it[StoreLocation.id].value,
                storeLocation = it[StoreLocation.code],
                manufacturingLineId = null,
                lotNo = "${it[LotNo.code]} | ${it[Sku.matCode]} | ${it[StoreLocation.code]}",
                lotNoId = it[LotNo.id].value,
                qty = it[StoreLocationHasLotNo.qty],
                refCode = it[LotNo.refCode],
                remark = "Cleanse condition room"
            )
        }
    }
}