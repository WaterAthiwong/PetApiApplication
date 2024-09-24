package com.champaca.inventorydata.pallet.usecase

import com.champaca.inventorydata.databasetable.LotNo
import com.champaca.inventorydata.databasetable.StoreLocationHasLotNo
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.storelocation.StoreLocationRepository
import com.champaca.inventorydata.pallet.request.ReceivePalletRequest
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.*
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class ReceivePalletUseCase(
    val dataSource: DataSource,
    val pileService: PileService,
    val wmsService: WmsService,
    val storeLocationRepository: StoreLocationRepository
) {

//    fun execute(sessionId: String, userId: String, request: ReceivePalletRequest): ReceivePalletResponse {
//        Database.connect(dataSource)
//
//        var errorType = PalletError.NONE
//        var errorMessage = ""
//        var collection: PileService.PalletCollection? = null
//        var receivedItems: List<MovingItem> = listOf()
//        var pileLotNos: List<Pair<PileDao, List<LotNoDao>>> = listOf()
//        var goodMovement: GoodMovementDao? = null
//        var storeLocationId = -1
//        transaction {
//            addLogger(ExposedInfoLogger)
//
//            collection = pileService.findPalletAndPilesAndCurrentLotNos(request.palletCode)
//            if (collection == null) {
//                errorType = PalletError.PALLET_NOT_FOUND
//                return@transaction
//            }
//
//            if (collection!!.isPalletEmpty()) {
//                errorType = PalletError.PALLET_IS_EMPTY
//                return@transaction
//            }
//
//            val toGoodMovement = GoodMovementDao.findById(request.goodMovementId)!!
//            if (toGoodMovement.transferGoodMovementId == null) {
//                errorType = PalletError.NO_LINK_TO_TRANSFER_GOOD_MOVEMENT
//                return@transaction
//            }
//            val fromGoodMovementId = toGoodMovement.transferGoodMovementId!!.value
//
//            val storeLocation = storeLocationRepository.getByCode(request.location)
//            if (storeLocation == null) {
//                errorType = PalletError.LOCATION_NOT_FOUND
//                return@transaction
//            }
//            storeLocationId = storeLocation.id.value
//
//            receivedItems = pileService.findReceivingItems(collection!!.getLotNoIds(), listOf(fromGoodMovementId))
//            val previousSkuGroup = receivedItems.first().skuName.substring(1, 3)
//            if (previousSkuGroup != request.skuGroupCode) {
//                val result = pileService.createNewReceivingItems(receivedItems, request.skuGroupCode)
//                if (result is ResultOf.Failure) {
//                    errorType = PalletError.NON_EXISTING_MATCODE
//                    errorMessage = result.message!!
//                    return@transaction
//                }
//                receivedItems = result as List<MovingItem>
//            }
//        }
//
//        if (errorType != PalletError.NONE) {
//            return ReceivePalletResponse.Failure(
//                errorType = errorType,
//                errorMessage = errorMessage
//            )
//        }
//
//        val itemMovements = createItemMovements(request, receivedItems, collection!!, storeLocationId)
//        val result = wmsService.itemMovement(sessionId, itemMovements)
//        if (result is ResultOf.Failure) {
//            return ReceivePalletResponse.Failure(
//                errorType = PalletError.WMS_VALIDATION_ERROR,
//                errorMessage = result.message
//            )
//        }
//
//        transaction {
//            addLogger(ExposedInfoLogger)
//
//            val lotRefCodes = itemMovements.map { it.refCode!! }
//            val idAndRefCodePairs = getLotNoIdAndRefCodePairs(lotRefCodes)
//
//
//        }
//
//
//    }

    private fun createItemMovements(request: ReceivePalletRequest,
                                    items: List<MovingItem>,
                                    collection: PileService.PalletCollection,
                                    storeLocationId: Int): List<WmsService.ItemMovementEntry> {
        val lotIdToItemMap = items.associateBy({ it.lotNoId }, { it })
        return collection.pileLotNos.map { pair ->
            val pile = pair.first
            val lotNos = pair.second

            lotNos.mapIndexed { index, lotNo ->
                val item = lotIdToItemMap[lotNo.id.value]!!
                val lotGroupRefCode = "${pile.code}_${request.processPrefix}"
                WmsService.ItemMovementEntry(
                    goodMovementType = GoodMovementType.GOODS_RECEIPT,
                    goodMovementId = request.goodMovementId,
                    skuId = item.skuId,
                    sku = item.skuName,
                    storeLocationId = storeLocationId,
                    storeLocation = request.location,
                    manufacturingLineId = request.manufacturingLineId,
                    qty = item.qty,
                    refCode = "${lotGroupRefCode}${(index + 1).toString().padStart(2, '0')}"
                )

            }
        }.flatten()
    }

    private fun getLotNoIdAndRefCodePairs(refCodes: List<String>): List<Pair<String, Int>> {
        val joins = StoreLocationHasLotNo.join(LotNo, JoinType.INNER) { StoreLocationHasLotNo.lotNoId eq LotNo.id }
        val query = joins.select(LotNo.refCode, LotNo.id)
            .where { (LotNo.refCode.inList(refCodes)) and (LotNo.status eq "A") and (StoreLocationHasLotNo.qty greater 0) }
        return query.map { resultRow -> Pair(resultRow[LotNo.refCode], resultRow[LotNo.id].value) }
    }

}