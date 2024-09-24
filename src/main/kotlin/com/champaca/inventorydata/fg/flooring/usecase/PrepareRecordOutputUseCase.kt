package com.champaca.inventorydata.fg.flooring.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.GoodMovement
import com.champaca.inventorydata.databasetable.dao.GoodMovementDao
import com.champaca.inventorydata.databasetable.dao.PileDao
import com.champaca.inventorydata.fg.flooring.model.MatCodeAttributeChangeType
import com.champaca.inventorydata.fg.flooring.request.PrepareRecordOutputRequest
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.fg.flooring.response.PrepareRecordOutputResponse
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.request.PileItem
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class PrepareRecordOutputUseCase(
    val dataSource: DataSource,
    val processOutputChanges: Map<Int, List<MatCodeAttributeChangeType>>,
    val processOutputLocation: Map<Int, String>,
    val processOutputNewGroup: Map<Int, List<String>>,
    val pileService: PileService
) {
    val logger = LoggerFactory.getLogger(PrepareRecordOutputUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: PrepareRecordOutputRequest): PrepareRecordOutputResponse {
        Database.connect(dataSource)
        val changes = processOutputChanges[request.processTypeId] ?: emptyList()

        var errorType: PileError = PileError.NONE
        lateinit var pile: PileDao
        var pileItems = emptyList<PileItem>()
        transaction {
            addLogger(exposedLogger)

            val pair = pileService.findPileAndCurrentLotNos(request.pileCode)
            if (pair == null) {
                errorType = PileError.PILE_NOT_FOUND
                logger.warn("Pile: ${request.pileCode} not found")
                return@transaction
            }

            pile = pair.first
            val pickingGoodMovementIdsResult = getPickingGoodMovementId(request, pile)
            if (pickingGoodMovementIdsResult is ResultOf.Failure) {
                errorType = PileError.NO_GOOD_RECEIPT_LINKS
                logger.warn(pickingGoodMovementIdsResult.message)
                return@transaction
            }

            val pickingGoodMovementIds = (pickingGoodMovementIdsResult as ResultOf.Success).value
            val lotIds = pair.second.map { it.id.value }
            val pickingItems = pileService.findReceivingItems(lotIds, pickingGoodMovementIds)
            pileItems = pickingItems.sortedBy { it.lotRefCode }.map {
                PileItem(
                    skuId = it.skuId,
                    matCode = it.matCode,
                    qty = it.qty,
                    lotRefCode = it.lotRefCode
                )
            }
        }

        if (errorType != PileError.NONE) {
            return PrepareRecordOutputResponse.Failure(errorType)
        }

        val location = processOutputLocation[request.processTypeId] ?: ""
        val newGroups = processOutputNewGroup[request.processTypeId] ?: emptyList()
        return PrepareRecordOutputResponse.Success(changes, location, pileItems, newGroups)
    }

    private fun getPickingGoodMovementId(request: PrepareRecordOutputRequest, pile: PileDao): ResultOf<List<Int>> {
        // ถ้าเป็นการรับจาก process ให้หา good movement ที่เป็น good receipt ของ good movement ปัจจุบัน
        val pickingGoodMovements  = GoodMovementDao.find { GoodMovement.goodReceiptGoodMovementId eq request.goodMovementId }.toList()
        if (pickingGoodMovements.isEmpty()) {
            // this is just to make sure that Data team link the good receipt with the picking order otherwise
            // the stock will stuck in manufacturing_line_has_lot_no
            return ResultOf.Failure("Pile: ${request.pileCode} has no picking good movement")
        }
        return ResultOf.Success(listOf(pile.goodMovementId.value))
    }
}