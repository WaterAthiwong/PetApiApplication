package com.champaca.inventorydata.pile.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.config.ConfigRepository
import com.champaca.inventorydata.masterdata.manufacturingline.ManufacturingLineRepository
import com.champaca.inventorydata.pile.response.CreatePileResponse
import com.champaca.inventorydata.masterdata.storelocation.StoreLocationRepository
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.request.CreatePileRequest
import com.champaca.inventorydata.utils.DateTimeUtil
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class CreatePileUseCase(
    val dataSource: DataSource,
    val storeLocationRepository: StoreLocationRepository,
    val wmsService: WmsService,
    val pileService: PileService,
    val configRepository: ConfigRepository,
    val manufacturingLineRepository: ManufacturingLineRepository,
    val dateTimeUtil: DateTimeUtil,
    val itemLockService: ItemLockService
) {
    val logger = LoggerFactory.getLogger(CreatePileUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, userId: String, request: CreatePileRequest): CreatePileResponse {
        val lockStr = "${request.processPrefix}_${request.goodMovementId}"
        try {
            itemLockService.lock(lockStr)
            Database.connect(dataSource)
            lateinit var pileCodes: List<String>
            var storeLocationId = -1
            var errorType = PileError.NONE
            var errorMessage = ""
            transaction {
                addLogger(exposedLogger)

                val storeLocation = storeLocationRepository.getByCode(request.location)
                if (storeLocation == null) {
                    errorType = PileError.LOCATION_NOT_FOUND
                    logger.warn("Location: ${request.location} not found")
                    return@transaction
                }
                storeLocationId = storeLocation.id.value

                val newPileResult = pileService.newPileCodeAndIncreaseRunningNumber(request.processPrefix, request.copies)
                if (newPileResult is ResultOf.Failure) {
                    errorType = PileError.PILE_CODE_EXIST
                    errorMessage = "Pile code ${newPileResult.message} already exists"
                    logger.warn("Pile ${newPileResult.message}: Pile code already exists")
                    return@transaction
                }
                pileCodes = (newPileResult as ResultOf.Success).value
            }

            if (errorType != PileError.NONE) {
                return CreatePileResponse.Failure(errorType = errorType, errorMessage = errorMessage)
            }

            val logGroupRefCodes =  pileCodes.map { pileCode -> "${pileCode}_${request.processPrefix}" }
            if (request.items.isNotEmpty()) {
                val itemMovements = createItemMovements(request, storeLocationId, logGroupRefCodes)
                val result = wmsService.receiveGmItem(sessionId, itemMovements)
                if (result is ResultOf.Failure) {
                    logger.warn("Pile ${pileCodes}: WMS validation error: ${(result as ResultOf.Failure).message}")
                    return CreatePileResponse.Failure(
                        errorType = PileError.WMS_VALIDATION_ERROR,
                        errorMessage = (result as ResultOf.Failure).message
                    )
                }
            }

            transaction {
                addLogger(exposedLogger)
                val lotRefCodes = logGroupRefCodes.map { logGroupRefCode ->
                    IntRange(1, request.items.size).toList()
                        .map { "${logGroupRefCode}${it.toString().padStart(2, '0')}" }
                }.flatten()
                val lotNoIds = getLotNoFromRefCode(lotRefCodes)
                    .groupBy { it.refCode.substringBefore("_") }
                    .mapValues { it.value.map { lotNo -> lotNo.id.value } }
                upsertPileRecords(userId, request, pileCodes, lotNoIds, storeLocationId)
            }

            return CreatePileResponse.Success(pileCode = pileCodes[0], pileCodes = pileCodes)
        } finally {
            itemLockService.unlock(lockStr)
        }
    }

    private fun createItemMovements(request: CreatePileRequest, storeLocationId: Int, logGroupRefCodes: List<String>): List<WmsService.ItemMovementEntry> {
        return logGroupRefCodes.map { logGroupRefCode ->
            request.items.mapIndexed { index, item ->
                WmsService.ItemMovementEntry(
                    goodMovementType = GoodMovementType.GOODS_RECEIPT,
                    goodMovementId = request.goodMovementId,
                    skuId = item.skuId,
                    sku = item.matCode,
                    storeLocationId = storeLocationId,
                    storeLocation = request.location,
                    manufacturingLineId = request.manufacturingLineId,
                    qty = item.qty.setScale(2, RoundingMode.HALF_UP),
                    refCode = "${logGroupRefCode}${(index + 1).toString().padStart(2, '0')}",
                    remark = request.remark
                )
            }
        }.flatten()
    }

    private fun getLotNoFromRefCode(refCodes: List<String>): List<LotNoDao> {
        return LotNoDao.find { (LotNo.refCode.inList(refCodes)) and (LotNo.status eq "A") }.toList()
    }

    private fun upsertPileRecords(userId: String, request: CreatePileRequest, pileCodes: List<String>,
                                  lotNoIdByPile: Map<String, List<Int>>, storeLocationId: Int) {
        val manufacturingLine = manufacturingLineRepository.findById(request.manufacturingLineId)
        val now = LocalDateTime.now()

        // Create Pile
        pileCodes.forEach { pileCode ->
            val lotNoIds = lotNoIdByPile[pileCode] ?: emptyList()
            val pileId = Pile.insertAndGetId {
                it[this.manufacturingLineId] = request.manufacturingLineId
                it[this.goodMovementId] = request.goodMovementId
                it[this.originGoodMovementId] = request.goodMovementId
                it[this.storeLocationId] = storeLocationId
                it[this.code] = pileCode
                it[this.processTypePrefix] = request.processPrefix
                it[this.orderNo] = request.orderNo
                it[this.lotSet] = 1
                it[this.type] = request.pileType
                it[this.remark] = request.remark
                it[this.createdAt] = now
                it[this.updatedAt] = now
                it[this.status] = "A"

                val extras = mutableMapOf<String, String>()
                if (manufacturingLine != null) {
                    extras[request.processPrefix] = manufacturingLine.name
                }
                if (request.refNo != null) {
                    extras["refNo"] = request.refNo
                }
                if (request.customer != null) {
                    extras["customer"] = request.customer
                }
                if (request.returned) {
                    extras["returned"] = "true"
                    extras["returnedJobNo"] = request.returnedJobNo ?: ""
                }
                if (extras.isNotEmpty()) {
                    it[this.extraAttributes] = extras
                }
            }

            // Bind Pile and LotNos
            pileService.addPileHasLotNos(pileId.value, lotNoIds, 1)

            // Record pile creation transaction
            pileService.addPileTransaction(
                pileId = pileId.value,
                toGoodMovementId = request.goodMovementId,
                userId = userId.toInt(),
                type = PileTransactionDao.CREATE,
                toLotNos = lotNoIds,
                remainingQty = request.items.map { it.qty.setScale(2, RoundingMode.HALF_UP) },
            )
        }
    }
}

