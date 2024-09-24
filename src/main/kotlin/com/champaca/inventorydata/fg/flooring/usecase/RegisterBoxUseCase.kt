package com.champaca.inventorydata.fg.flooring.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.FinishedGoodSticker
import com.champaca.inventorydata.databasetable.FinishedGoodStickerHasSku
import com.champaca.inventorydata.databasetable.LotNo
import com.champaca.inventorydata.databasetable.Pile
import com.champaca.inventorydata.databasetable.dao.FinishedGoodStickerDao
import com.champaca.inventorydata.databasetable.dao.LotNoDao
import com.champaca.inventorydata.databasetable.dao.PileDao
import com.champaca.inventorydata.databasetable.dao.PileDao.Companion.FG_BOX
import com.champaca.inventorydata.databasetable.dao.PileTransactionDao
import com.champaca.inventorydata.fg.flooring.request.RegisterBoxRequest
import com.champaca.inventorydata.fg.flooring.response.RegisterBoxResponse
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.manufacturingline.ManufacturingLineRepository
import com.champaca.inventorydata.masterdata.storelocation.StoreLocationRepository
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.usecase.CreatePileUseCase
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class RegisterBoxUseCase(
    val dataSource: DataSource,
    val storeLocationRepository: StoreLocationRepository,
    val itemLockService: ItemLockService,
    val wmsService: WmsService,
    val pileService: PileService,
    val manufacturingLineRepository: ManufacturingLineRepository
) {
    val logger = LoggerFactory.getLogger(CreatePileUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, userId: String, request: RegisterBoxRequest): RegisterBoxResponse {
        val lockStr = "${request.departmentPrefix}_${request.goodMovementId}"
        try {
            itemLockService.lock(lockStr)
            Database.connect(dataSource)

            var storeLocationId = -1
            var errorType = PileError.NONE
            var errorMessage = ""
            lateinit var sticker: FinishedGoodStickerDao
            var itemMovements = listOf<WmsService.ItemMovementEntry>()
            transaction {
                addLogger(exposedLogger)

                val storeLocation = storeLocationRepository.getByCode(request.location)
                if (storeLocation == null) {
                    errorType = PileError.LOCATION_NOT_FOUND
                    logger.warn("Location: ${request.location} not found")
                    return@transaction
                }
                storeLocationId = storeLocation.id.value

                val pile = PileDao.find { (Pile.code eq request.boxCode) and (Pile.status eq "A") }.firstOrNull()
                if (pile != null) {
                    // ที่ต้องใส่ตรงนี้มาดักไว้ตรงนี้เพราะว่าเคยเจอเคสที่เลข running จาก cpc_config มันจู่ๆก็ย้อนกลับไปสิบกว่าเลข
                    // ทำให้สร้างกองไม้ที่มีเลขซ้ำ มั่วไปยาวๆเลย กว่าจะแก้เสร็จเสียเวลามาก
                    errorType = PileError.PILE_CODE_EXIST
                    errorMessage = "Box code: ${request.boxCode} already exist"
                    return@transaction
                }

                val tempStickers = FinishedGoodStickerDao.find { FinishedGoodSticker.code eq request.boxCode }
                if (tempStickers == null) {
                    errorType = PileError.PILE_NOT_FOUND
                    errorMessage = "Sticker code: ${request.boxCode} not found"
                    return@transaction
                }
                sticker = tempStickers.first()
                itemMovements = createItemMovements(request, sticker, storeLocationId)
            }

            if (errorType != PileError.NONE) {
                return RegisterBoxResponse.Failure(errorType = errorType, errorMessage = errorMessage)
            }

            val result = wmsService.receiveGmItem(sessionId, itemMovements)
            if (result is ResultOf.Failure) {
                logger.warn("Box ${request.boxCode}: WMS validation error: ${(result as ResultOf.Failure).message}")
                return RegisterBoxResponse.Failure(
                    errorType = PileError.WMS_VALIDATION_ERROR,
                    errorMessage = (result as ResultOf.Failure).message
                )
            }

            var boxCount = 0
            var itemCount = 0
            transaction {
                addLogger(exposedLogger)
                val lotNos = getLotNoFromRefCode(itemMovements.map { it.refCode!! })
                upsertPileRecords(userId, request, lotNos.map { it.id.value }, storeLocationId, sticker, itemMovements)
                pileService.getPileAndItemCount(request.goodMovementId).apply {
                    boxCount = this.first
                    itemCount = this.second
                }
            }

            return RegisterBoxResponse.Success(boxCount, itemCount)
        } finally {
            itemLockService.unlock(lockStr)
        }
    }

    private fun createItemMovements(request: RegisterBoxRequest, sticker: FinishedGoodStickerDao, storeLocationId: Int): List<WmsService.ItemMovementEntry> {
        val skuQtys = getSkuAndQtyForBox(sticker.id.value)
        return skuQtys.mapIndexed { index, (skuId, qty) ->
            WmsService.ItemMovementEntry(
                goodMovementType = GoodMovementType.GOODS_RECEIPT,
                goodMovementId = request.goodMovementId,
                skuId = skuId,
                sku = "SKU",
                storeLocationId = storeLocationId,
                storeLocation = request.location,
                manufacturingLineId = request.manufacturingLineId,
                qty = qty.setScale(2, RoundingMode.HALF_UP),
                refCode = "${sticker.code}_${request.departmentPrefix}${(index + 1).toString().padStart(2, '0')}",
                remark = ""
            )
        }
    }

    private fun getLotNoFromRefCode(refCodes: List<String>): List<LotNoDao> {
        return LotNoDao.find { (LotNo.refCode.inList(refCodes)) and (LotNo.status eq "A") }.toList()
    }

    private fun getSkuAndQtyForBox(stickerId: Int): List<Pair<Int, BigDecimal>> {
        val query = FinishedGoodStickerHasSku.selectAll().where {
            (FinishedGoodStickerHasSku.stickerId eq stickerId) and (FinishedGoodStickerHasSku.status eq "A")
        }
        return query.map { row ->
            Pair(row[FinishedGoodStickerHasSku.skuId].value, row[FinishedGoodStickerHasSku.qty])
        }
    }

    private fun upsertPileRecords(userId: String, request: RegisterBoxRequest,
                                  lotNoIds: List<Int>, storeLocationId: Int, sticker: FinishedGoodStickerDao,
                                  itemMovements: List<WmsService.ItemMovementEntry>) {
        val manufacturingLine = manufacturingLineRepository.findById(request.manufacturingLineId)
        val now = LocalDateTime.now()

        // Create Pile
        val pileId = Pile.insertAndGetId {
            it[this.manufacturingLineId] = request.manufacturingLineId
            it[this.goodMovementId] = request.goodMovementId
            it[this.originGoodMovementId] = request.goodMovementId
            it[this.storeLocationId] = storeLocationId
            it[this.code] = request.boxCode
            it[this.processTypePrefix] = request.departmentPrefix
            it[this.lotSet] = 1
            it[this.type] = FG_BOX
            it[this.createdAt] = now
            it[this.updatedAt] = now
            it[this.status] = "A"

            val extras = mutableMapOf<String, String>()
            if (manufacturingLine != null) {
                extras[request.departmentPrefix] = manufacturingLine.name
            }
            extras["stickerId"] = sticker.id.value.toString()
            it[this.extraAttributes] = extras
        }

        // Bind Pile and LotNos
        pileService.addPileHasLotNos(pileId.value, lotNoIds, 1)

        // Record pile creation transaction
        pileService.addPileTransaction(pileId = pileId.value,
            toGoodMovementId = request.goodMovementId,
            userId = userId.toInt(),
            type = PileTransactionDao.CREATE,
            toLotNos = lotNoIds,
            remainingQty = itemMovements.map { it.qty.setScale(2, RoundingMode.HALF_UP) },
        )

        // Update sticker so that we know which sticker binds to which pile
        sticker.pileId = pileId.value
        sticker.updatedAt = now
    }
}