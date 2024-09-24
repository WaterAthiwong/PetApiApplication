package com.champaca.inventorydata.pallet.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.Pallet
import com.champaca.inventorydata.databasetable.Pile
import com.champaca.inventorydata.databasetable.dao.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.pallet.PalletError
import com.champaca.inventorydata.pallet.request.PickPalletRequest
import com.champaca.inventorydata.pallet.response.PickPalletResponse
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.usecase.toPickedGoodMovement
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class PickPalletUseCase(
    val dataSource: DataSource,
    val pileService: PileService,
    val wmsService: WmsService,
    val itemLockService: ItemLockService
) {

    fun execute(sessionId: String, userId: String, request: PickPalletRequest): PickPalletResponse {
        try {
            itemLockService.lock(request.palletCode)   // Lock the pallet
            Database.connect(dataSource)

            var errorType = PalletError.NONE
            var collection: PileService.PalletCollection? = null
            var goodMovement: GoodMovementDao? = null
            var pickedItems: List<MovingItem> = listOf()
            transaction {
                addLogger(ExposedInfoLogger)

                collection = pileService.findPalletAndPilesAndCurrentLotNos(request.palletCode)
                if (collection == null) {
                    errorType = PalletError.PALLET_NOT_FOUND
                    return@transaction
                }

                val pallet = collection!!.pallet
                val pileLotNos = collection!!.pileLotNos
                if (collection!!.isPalletEmpty()) {
                    errorType = PalletError.PALLET_IS_EMPTY
                    return@transaction
                }

                pickedItems = pileService.findItemsInStorageArea(collection!!.getLotNoIds())
                goodMovement = GoodMovementDao.findById(pileLotNos.first().first.goodMovementId)
            }

            if (errorType != PalletError.NONE) {
                return PickPalletResponse.Failure(
                    goodMovement = goodMovement?.toPickedGoodMovement(),
                    items = pickedItems,
                    errorType = errorType
                )
            }

            val itemMovements = createItemMovements(request, pickedItems)
            val result = wmsService.pickGmItem(sessionId, itemMovements)
            if (result is ResultOf.Failure) {
                PickPalletResponse.Failure(
                    errorType = PalletError.WMS_VALIDATION_ERROR,
                    errorMessage = result.message,
                    goodMovement = goodMovement?.toPickedGoodMovement(),
                    items = pickedItems
                )
            }

            transaction {
                addLogger(ExposedInfoLogger)
                upsertPalletRecords(userId = userId,
                    request = request,
                    collection = collection!!,
                    lotNoIdToMovingItemMap = pickedItems.associateBy({ it.lotNoId }, { it })
                )
            }

            return PickPalletResponse.Success(
                pileCount = collection!!.pileLotNos.size,
                goodMovement = goodMovement!!.toPickedGoodMovement(),
                items = pickedItems
            )
        } finally {
            itemLockService.unlock(request.palletCode)   // Unlock the pallet
        }
    }

    private fun createItemMovements(request: PickPalletRequest, items: List<MovingItem>): List<WmsService.ItemMovementEntry> {
        return items.map { pickingData ->
            val lotName = "${pickingData.lotCode} | ${pickingData.matCode} | ${pickingData.lotRefCode} | ${pickingData.storeLocationCode}"

            WmsService.ItemMovementEntry(
                goodMovementType = GoodMovementType.PICKING_ORDER,
                goodMovementId = request.goodMovementId,
                skuId = pickingData.skuId,
                storeLocationId = pickingData.storeLocationId,
                manufacturingLineId = request.manufacturingLineId,
                qty = pickingData.qty,
                lotNoId = pickingData.lotNoId,
                lotNo = lotName
            )
        }
    }

    private fun upsertPalletRecords(userId: String,
                                    request: PickPalletRequest,
                                    collection: PileService.PalletCollection,
                                    lotNoIdToMovingItemMap: Map<Int, MovingItem>) {
        val pallet = collection.pallet
        val pileLotNos = collection.pileLotNos
        val now = LocalDateTime.now()
        val pileIds = pileLotNos.map { it.first.id.value }
        Pallet.update({ Pallet.id eq pallet.id.value }) {
            it[this.updatedAt] = now
            it[this.storeLocationId] = null
        }

        Pile.update({ Pile.id inList pileIds }) {
            it[this.goodMovementId] = request.goodMovementId
            it[this.updatedAt] = now
        }

        pileLotNos.forEach { pileLotNo ->
            val pile = pileLotNo.first
            val lotNos = pileLotNo.second
            val lotNoIds = lotNos.map { it.id.value }
            val pickedItems = lotNoIds.map { lotNoIdToMovingItemMap[it]!! }

            // Record Pile pick for process transaction
            pileService.addPileTransaction(
                pileId = pile.id.value,
                fromGoodMovementId = pile.goodMovementId.value,
                toGoodMovementId = request.goodMovementId,
                palletId = pallet.id.value,
                userId = userId.toInt(),
                type = PileTransactionDao.PICK,
                fromLotNos = pickedItems.map { it.lotNoId },
                movingQty = pickedItems.map { it.qty },
                remainingQty = List(pickedItems.size) { 0.toBigDecimal() }
            )
        }
    }
}