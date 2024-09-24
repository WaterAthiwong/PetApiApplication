package com.champaca.inventorydata.fg.flooring.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.manufacturingline.ManufacturingLineRepository
import com.champaca.inventorydata.fg.flooring.model.MatCodeAttributeChangeType
import com.champaca.inventorydata.masterdata.sku.SkuRepository
import com.champaca.inventorydata.masterdata.storelocation.StoreLocationRepository
import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.fg.flooring.request.RecordOutputRequest
import com.champaca.inventorydata.fg.flooring.response.RecordOutputResponse
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class RecordOutputUseCase(
    val dataSource: DataSource,
    val pileService: PileService,
    val wmsService: WmsService,
    val storeLocationRepository: StoreLocationRepository,
    val itemLockService: ItemLockService,
    val manufacturingLineRepository: ManufacturingLineRepository,
    val processOutputChanges: Map<Int, List<MatCodeAttributeChangeType>>,
    val skuRepository: SkuRepository
) {
    companion object {
        val MATCODE_REGEX = "(\\d{1})([A-Z]\\d{1})([A-Z]+)(\\d{1})([A-Z]*)-([0-9.]+)X([0-9.]+)X([0-9.]+)".toRegex()
    }

    val logger = LoggerFactory.getLogger(RecordOutputUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, userId: String, request: RecordOutputRequest): RecordOutputResponse {
        try {
            itemLockService.lock(request.pileCode)   // Lock the pile
            Database.connect(dataSource)

            val changes = processOutputChanges[request.processTypeId] ?: emptyList()
            var errorType = PileError.NONE
            var errorMessage = ""
            var receivedItems: List<MovingItem> = listOf()
            lateinit var pile: PileDao
            var storeLocationId = -1
            var itemMovements = listOf<WmsService.ItemMovementEntry>()
            val lotGroupRefCode = "${request.pileCode}_${request.processPrefix}"
            val hasRework = request.processingResults.filter { it.reworkQty > BigDecimal.ZERO }.isNotEmpty()
            val reworkPileCode = "${request.pileCode}.${request.processPrefix}"
            val reworkLotGroupRefCode = "${reworkPileCode}_${request.processPrefix}"
            transaction {
                addLogger(exposedLogger)

                if (request.processingResults.size != request.items.size) {
                    errorType = PileError.INVALID_REQUEST
                    errorMessage = "New quantity size does not match the item size"
                    logger.warn("Pile: ${request.pileCode}, ${errorMessage}")
                    return@transaction
                }

                val pair = pileService.findPileAndCurrentLotNos(request.pileCode)
                if (pair == null) {
                    errorType = PileError.PILE_NOT_FOUND
                    logger.warn("Pile: ${request.pileCode} not found")
                    return@transaction
                }

                pile = pair.first
                if (pile.goodMovementId.value == request.goodMovementId) {
                    // If the pile's latest goodMovementId equals to the request's goodMovementId, this is probably because
                    // the user scans the same pile twice. Hence, we block the second pile receive
                    errorType = PileError.PILE_HAS_BEEN_RECEIVED
                    errorMessage = "Pile has been recorded at this good movement ${request.goodMovementId}"
                    logger.warn("Pile: ${request.pileCode} has been been recorded at this good movement ${request.goodMovementId}")
                    return@transaction
                }

                val storeLocation = storeLocationRepository.getByCode(request.location)
                if (storeLocation == null) {
                    errorType = PileError.LOCATION_NOT_FOUND
                    logger.warn("Pile: ${request.pileCode}, Location: ${request.location} not found")
                    return@transaction
                }
                storeLocationId = storeLocation.id.value

                val matCodeMap = modifyMatCodes(request, changes)
                val nonExistingMatCodes = skuRepository.findNonExistingMatCodes(matCodeMap.values.toList())
                if (nonExistingMatCodes.isNotEmpty()) {
                    errorType = PileError.NON_EXISTING_MATCODE
                    errorMessage = " ${nonExistingMatCodes.joinToString(", ")}"
                    logger.warn("Pile: ${request.pileCode} has non-existing matCode: ${errorMessage}")
                    return@transaction
                }

                itemMovements = createItemMovementsForFinishedItems(request, storeLocationId, lotGroupRefCode, matCodeMap)
            }

            if (errorType != PileError.NONE) {
                return RecordOutputResponse.Failure(errorType = errorType, errorMessage = errorMessage)
            }

            val reworkItemMovements = createReworkItemMovements(request, storeLocationId, reworkLotGroupRefCode)
            val result = wmsService.receiveGmItem(sessionId, itemMovements + reworkItemMovements)
            if (result is ResultOf.Failure) {
                logger.warn("Pile ${request.pileCode}: WMS validation error: ${result.message}")
                return RecordOutputResponse.Failure(
                    errorType = PileError.WMS_VALIDATION_ERROR,
                    errorMessage = (result as ResultOf.Failure).message
                )
            }

            var pileCount = 0
            var itemCount = 0
            transaction {
                // Record Transaction and changes
                addLogger(exposedLogger)
                val lotNoIds = getLotNoIdFromRefCode(itemMovements.map { it.refCode!! })
                val reworkLotNoIds = getLotNoIdFromRefCode(reworkItemMovements.map { it.refCode!! })
                upsertPileRecords(userId, request, pile, receivedItems, lotNoIds, storeLocationId)
                if (hasRework) {
                    insertReworkPileRecords(userId, request, pile, reworkPileCode, reworkLotNoIds, storeLocationId)
                }
                insertDefects(request, pile)
                pileService.getPileAndItemCount(request.goodMovementId).apply {
                    pileCount = this.first
                    itemCount = this.second
                }
            }



            return RecordOutputResponse.Success(
                receivingItems = receivedItems,
                pileCount = pileCount,
                itemCount = itemCount,
                reworkPileCode = if (hasRework) reworkPileCode else null)
        } finally {
            itemLockService.unlock(request.pileCode)   // Unlock the pile
        }
    }

    private fun getLotNoIdFromRefCode(refCodes: List<String>): List<Int> {
        val joins = StoreLocationHasLotNo.join(LotNo, JoinType.INNER) { StoreLocationHasLotNo.lotNoId eq LotNo.id }
        val query = joins.select(LotNo.id)
            .where { (LotNo.refCode.inList(refCodes)) and (LotNo.status eq "A") and (StoreLocationHasLotNo.qty greater 0) }
        return query.map { resultRow -> resultRow[LotNo.id].value }
    }

    private fun upsertPileRecords(userId: String, request: RecordOutputRequest, pile: PileDao,
                                  receivedItems: List<MovingItem>, lotNoIds: List<Int>, storeLocationId: Int) {
        val manufacturingLine = manufacturingLineRepository.findById(request.manufacturingLineId)
        val newLotSet = pile.lotSet + 1

        // Update pile with the new good movement and lot set
        Pile.update({Pile.id eq pile.id.value}) {
            it[this.storeLocationId] = storeLocationId
            it[this.goodMovementId] = request.goodMovementId
            it[this.lotSet] = newLotSet
            it[this.updatedAt] = LocalDateTime.now()

            if (manufacturingLine != null) {
                val extraAttributes = pile.extraAttributes?.toMutableMap() ?: mutableMapOf()
                extraAttributes[request.processPrefix] = manufacturingLine.name
                it[this.extraAttributes] = extraAttributes
            }
        }

        // Bind pile with a new set of lotNos
        pileService.addPileHasLotNos(pile.id.value, lotNoIds, newLotSet)

        // Record pile receive from the process
        pileService.addPileTransaction(pileId = pile.id.value,
            fromGoodMovementId = pile.goodMovementId.value,
            toGoodMovementId = request.goodMovementId,
            userId = userId.toInt(),
            type = PileTransactionDao.RECEIVE,
            fromLotNos = receivedItems.map { item -> item.lotNoId },
            toLotNos = lotNoIds,
            remainingQty = receivedItems.map { item -> item.qty },
        )
    }

    private fun insertReworkPileRecords(userId: String, request: RecordOutputRequest, originalPile: PileDao,
                                        reworkPileCode: String, lotNoIds: List<Int>, storeLocationId: Int) {
        val manufacturingLine = manufacturingLineRepository.findById(request.manufacturingLineId)
        val now = LocalDateTime.now()

        // Create Pile
        val pileId = Pile.insertAndGetId {
            it[this.manufacturingLineId] = request.manufacturingLineId
            it[this.goodMovementId] = request.goodMovementId
            it[this.originGoodMovementId] = request.goodMovementId
            it[this.storeLocationId] = storeLocationId
            it[this.code] = reworkPileCode
            it[this.processTypePrefix] = request.processPrefix
            it[this.orderNo] = originalPile.orderNo
            it[this.lotSet] = 1
            it[this.type] = PileDao.REWORK
            it[this.remark] = originalPile.remark
            it[this.createdAt] = now
            it[this.updatedAt] = now
            it[this.status] = "A"

            val extras = mutableMapOf<String, String>()
            extras[request.processPrefix] = manufacturingLine!!.name
            extras["parentPileId"] = originalPile.id.value.toString()
            it[this.extraAttributes] = extras
        }

        // Bind Pile and LotNos
        pileService.addPileHasLotNos(pileId.value, lotNoIds, 1)

        // Record pile creation transaction
        val qtys = request.processingResults.filter { it.reworkQty > BigDecimal.ZERO }.map { it.reworkQty }
        pileService.addPileTransaction(
            pileId = pileId.value,
            toGoodMovementId = request.goodMovementId,
            userId = userId.toInt(),
            type = PileTransactionDao.CREATE,
            toLotNos = lotNoIds,
            remainingQty = qtys.map { it.setScale(2, RoundingMode.HALF_UP) },
        )
    }

    private fun insertDefects(request: RecordOutputRequest, pile: PileDao) {
        val now = LocalDateTime.now()
        val defects = mutableListOf<Triple<Int, BigDecimal, String>>()
        request.processingResults.forEachIndexed { index, result ->
            val item = request.items[index]
            val rejectedQty = item.qty - (result.finishedQty + result.reworkQty)
            if (rejectedQty > BigDecimal.ZERO) {
                defects.add(Triple(item.skuId, rejectedQty, DefectDao.REJECTED))
            }

            if (result.reworkQty > BigDecimal.ZERO) {
                defects.add(Triple(item.skuId, result.reworkQty, DefectDao.REWORK))
            }
        }
        Defect.batchInsert(defects) {
            this[Defect.pileId] = pile.id.value
            this[Defect.manufacturingLineId] = request.manufacturingLineId!!
            this[Defect.skuId] = it.first
            this[Defect.type] = it.third
            this[Defect.qty] = it.second
            this[Defect.createdAt] = now
            this[Defect.status] = "A"
        }
    }

    private fun modifyMatCodes(request: RecordOutputRequest, changes: List<MatCodeAttributeChangeType>): Map<String, String> {
        if (changes.isNotEmpty()) {
            val oldMatCodes = request.items.map { it.matCode }
            val results = mutableMapOf<String, String>()
            oldMatCodes.forEachIndexed { index, oldMatCode ->
                val matchResult = MATCODE_REGEX.find(oldMatCode) ?: return@forEachIndexed
                val processingResult = request.processingResults[index]
                val groups = matchResult.groupValues
                var skuGroup = groups[1] + groups[2]
                val species = groups[3]
                val fsc = groups[4]
                val grade = groups[5]
                var thickness = groups[6]
                var width = groups[7]
                var length = groups[8]

                changes.forEach { change ->
                    when (change) {
                        MatCodeAttributeChangeType.NEW_THICKNESS -> {
                            thickness = processingResult.newThickness
                        }

                        MatCodeAttributeChangeType.NEW_WIDTH -> {
                            width = processingResult.newWidth
                        }

                        MatCodeAttributeChangeType.NEW_LENGTH -> {
                            length = processingResult.newLength
                        }

                        MatCodeAttributeChangeType.INCREASE_MAIN_SKU_GROUP_FROM_3_TO_4 -> {
                            if (groups[1] == "3") {
                                skuGroup = "4" + groups[2]
                            }
                        }

                        MatCodeAttributeChangeType.NEW_SKU_GROUP -> {
                            skuGroup = request.newGroup
                        }
                    }
                }
                results[oldMatCode] = "$skuGroup$species$fsc$grade-${thickness}X${width}X${length}"
            }
            return results
        }

        return request.items.map { it.matCode to it.matCode }.toMap()
    }

    private fun createItemMovementsForFinishedItems(request: RecordOutputRequest, storeLocationId: Int, lotGroupRefCode: String,
                                                    matCodeMap: Map<String, String>): List<WmsService.ItemMovementEntry> {
        val skuIdMap = skuRepository.getSkuIdsFromMatCodes(matCodeMap.values.toList())
        val results = mutableListOf<WmsService.ItemMovementEntry>()
        request.items.forEachIndexed { index, item ->
            val newMatCode = matCodeMap[item.matCode]!!
            val newSkuId = skuIdMap[newMatCode]!!
            val processResult = request.processingResults[index]
            if (processResult.finishedQty == BigDecimal.ZERO) {
                return@forEachIndexed
            }
            results.add(
                WmsService.ItemMovementEntry(
                    goodMovementType = GoodMovementType.GOODS_RECEIPT,
                    goodMovementId = request.goodMovementId,
                    skuId = newSkuId,
                    sku = newMatCode,
                    storeLocationId = storeLocationId,
                    storeLocation = request.location,
                    manufacturingLineId = request.manufacturingLineId,
                    qty = processResult.finishedQty.setScale(2, RoundingMode.HALF_UP),
                    refCode = "${lotGroupRefCode}${(index + 1).toString().padStart(2, '0')}"
                )
            )
        }
        return results
    }

    private fun createReworkItemMovements(request: RecordOutputRequest, storeLocationId: Int, lotGroupRefCode: String):
            List<WmsService.ItemMovementEntry> {
        val results = mutableListOf<WmsService.ItemMovementEntry>()
        request.items.forEachIndexed { index, item ->
            val processingResult = request.processingResults[index]
            if (processingResult.reworkQty == BigDecimal.ZERO) {
                return@forEachIndexed
            }
            results.add(
                WmsService.ItemMovementEntry(
                    goodMovementType = GoodMovementType.GOODS_RECEIPT,
                    goodMovementId = request.goodMovementId,
                    skuId = item.skuId,
                    sku = item.matCode,
                    storeLocationId = storeLocationId,
                    storeLocation = request.location,
                    manufacturingLineId = request.manufacturingLineId,
                    qty = processingResult.reworkQty.setScale(2, RoundingMode.HALF_UP),
                    refCode = "${lotGroupRefCode}${(index + 1).toString().padStart(2, '0')}"
                )
            )
        }
        return results
    }
}