package com.champaca.inventorydata.goodmovement.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.GoodMovement
import com.champaca.inventorydata.databasetable.UserHasProcessType
import com.champaca.inventorydata.databasetable.dao.GoodMovementDao
import com.champaca.inventorydata.goodmovement.GoodMovementError
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.goodmovement.request.CreateWholeProductLineGoodMovementRequest
import com.champaca.inventorydata.goodmovement.response.CreateWholeProductLineGoodMovementResponse
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class CreateWholeProductLineGoodMovementUseCase(
    val dataSource: DataSource,
    val wmsService: WmsService,
    val itemLockService: ItemLockService
) {
    val logger = LoggerFactory.getLogger(CreateWholeProductLineGoodMovementUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, userId: String, request: CreateWholeProductLineGoodMovementRequest): CreateWholeProductLineGoodMovementResponse {
        try {
            itemLockService.lock(userId)
            Database.connect(dataSource)
            var errorType = GoodMovementError.NONE
            var errorMessage = ""
            transaction {
                addLogger(exposedLogger)
                val approvalRights = getApprovalRights(userId, request.lines.map { it.processTypeId })
                if (approvalRights.size != request.lines.size) {
                    errorType = GoodMovementError.NO_APPROVAL_RIGHT
                    errorMessage = "User does not have approval rights for all process types"
                    logger.warn(errorMessage)
                    return@transaction
                }
            }

            if (errorType != GoodMovementError.NONE) {
                return CreateWholeProductLineGoodMovementResponse.Failure(errorType, errorMessage)
            }


            val goodMovementDatas = request.toGoodMovementData()
            goodMovementDatas.forEach {
                val result = wmsService.createGoodMovement(sessionId, it)
                if (result is ResultOf.Failure) {
                    logger.warn("Failed to create good movement: ${result.message}")
                    return CreateWholeProductLineGoodMovementResponse.Failure(
                        errorType = GoodMovementError.WMS_VALIDATION_ERROR,
                        errorMessage = result.message
                    )
                }
            }

            var goodMovements = listOf<GoodMovementDao>()
            transaction {
                addLogger(exposedLogger)
                goodMovements = GoodMovementDao.find { GoodMovement.userId eq userId.toInt() }
                    .orderBy(GoodMovement.id to SortOrder.DESC)
                    .limit(goodMovementDatas.size)
                    .toList()
            }
            goodMovements.forEach { goodMovement ->
                if (goodMovement.type == GoodMovementType.PICKING_ORDER.wmsName) {
                    val result = wmsService.approveGoodMovement(sessionId, goodMovement.id.value)
                    if (result is ResultOf.Failure) {
                        logger.warn("Failed to approve good movement ${goodMovement.id.value}: ${result.message}")
                        return CreateWholeProductLineGoodMovementResponse.Failure(
                            errorType = GoodMovementError.WMS_VALIDATION_ERROR,
                            errorMessage = result.message
                        )
                    }
                }
            }

            val pairs = goodMovements.groupBy { it.manufacturingLineId }.mapValues {
                val goodReceiptId = it.value.find { it.type == GoodMovementType.GOODS_RECEIPT.wmsName }!!.id.value
                val pickingId = it.value.find { it.type == GoodMovementType.PICKING_ORDER.wmsName }!!.id.value
                goodReceiptId to pickingId
            }.values.toList()
            pairs.forEach { pair ->
                wmsService.addReferenceGoodMovement(sessionId, pair.first, listOf(pair.second))
            }

            return CreateWholeProductLineGoodMovementResponse.Success(goodMovements.size)
        } finally {
            itemLockService.unlock(userId)
        }
    }

    private fun getApprovalRights(userId: String, processTypeIds: List<Int>): List<Int> {
        val query = UserHasProcessType.select(UserHasProcessType.processTypeId)
            .where { (UserHasProcessType.userId eq userId.toInt()) and (UserHasProcessType.processTypeId inList processTypeIds) }
        return query.map { it[UserHasProcessType.processTypeId] }
    }
}