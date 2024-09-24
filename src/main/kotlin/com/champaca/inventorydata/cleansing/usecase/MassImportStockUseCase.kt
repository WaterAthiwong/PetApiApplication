package com.champaca.inventorydata.cleansing.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.cleansing.request.MassImportStockRequest
import com.champaca.inventorydata.databasetable.LotNo
import com.champaca.inventorydata.databasetable.Pile
import com.champaca.inventorydata.databasetable.PileHasLotNo
import com.champaca.inventorydata.databasetable.Sku
import com.champaca.inventorydata.databasetable.dao.LotNoDao
import com.champaca.inventorydata.databasetable.dao.PileDao
import com.champaca.inventorydata.databasetable.dao.SkuDao
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.storelocation.StoreLocationRepository
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class MassImportStockUseCase(
    val dataSource: DataSource,
    val wmsService: WmsService,
    val storeLocationRepository: StoreLocationRepository
) {

    fun execute(sessionId: String, userId: String, request: MassImportStockRequest) {
        Database.connect(dataSource)

        var itemMovements = listOf<WmsService.ItemMovementEntry>()
        lateinit var pileByCode: Map<String, PileDao>
        transaction {
            addLogger(ExposedInfoLogger)

            pileByCode = PileDao.find { (Pile.code inList request.stocks.map { it.pileCode }) and (Pile.status eq "A")}
                .toList()
                .associateBy { it.code }

            val skuByMatCode = SkuDao.find { Sku.matCode inList request.stocks.map { it.matCode } }
                .toList()
                .associateBy { it.matCode }

            val stockByPile = request.stocks.groupBy { it.pileCode }
            stockByPile.forEach { pileCode, stocks ->
                stocks.forEachIndexed { index, stock ->
                    stock.refCode = "${pileCode}_${request.departmentPrefix}${(index + 1).toString().padStart(2, '0')}"
                }
            }

            val storeLocation = storeLocationRepository.getByCode(request.location)
            if (storeLocation == null) {
                println("Location: ${request.location} not found")
                return@transaction
            }
            val storeLocationId = storeLocation.id.value

            itemMovements = request.stocks.map { stock ->
                println(stock.matCode)
                val sku = skuByMatCode[stock.matCode]!!
                WmsService.ItemMovementEntry(
                    goodMovementType = GoodMovementType.GOODS_RECEIPT,
                    goodMovementId = request.goodMovementId,
                    skuId = sku.id.value,
                    storeLocationId = storeLocationId,
                    manufacturingLineId = null,
                    qty = stock.qty,
                    refCode = stock.refCode,
                )
            }
        }

//        wmsService.receiveGmItem(sessionId, itemMovements)

        transaction {
            addLogger(ExposedInfoLogger)
            val stocks = fillReceivingLotNoIds(request.stocks)
            val now = LocalDateTime.now()
            PileHasLotNo.batchInsert(stocks) { stock ->
                val pile = pileByCode[stock.pileCode]!!
                this[PileHasLotNo.lotNoId] = stock.lotNoId
                this[PileHasLotNo.pileId] = pile.id.value
                this[PileHasLotNo.lotSet] = 1
                this[PileHasLotNo.createdAt] = now
            }
        }
    }

    private fun fillReceivingLotNoIds(stocks: List<MassImportStockRequest.MassStock>): List<MassImportStockRequest.MassStock> {
        val lotNoDaos = LotNoDao.find { (LotNo.refCode inList stocks.map { it.refCode }) and (LotNo.status eq "A") }
            .toList()
            .associateBy { it.refCode }
        stocks.forEach { item ->
            val lotNoDao = lotNoDaos[item.refCode]!!
            item.apply {
                lotNoId = lotNoDao.id.value
            }
        }
        return stocks
    }
}