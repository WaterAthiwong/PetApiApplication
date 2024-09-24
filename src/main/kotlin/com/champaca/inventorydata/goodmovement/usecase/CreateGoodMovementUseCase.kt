package com.champaca.inventorydata.goodmovement.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.GoodMovement
import com.champaca.inventorydata.databasetable.dao.*
import com.champaca.inventorydata.goodmovement.GoodMovementError
import com.champaca.inventorydata.goodmovement.GoodMovementService
import com.champaca.inventorydata.goodmovement.model.GoodMovementData
import com.champaca.inventorydata.goodmovement.request.CreateGoodMovementRequest
import com.champaca.inventorydata.goodmovement.response.CreateGoodMovementResponse
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class CreateGoodMovementUseCase(
    val dataSource: DataSource,
    val wmsService: WmsService,
    val itemLockService: ItemLockService,
    val goodMovementService: GoodMovementService
) {
    val logger = LoggerFactory.getLogger(CreateGoodMovementUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)
    val productionDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    fun execute(sessionId: String, userId: String, request: CreateGoodMovementRequest): CreateGoodMovementResponse {
        try {
            itemLockService.lock(userId)
            val result = wmsService.createGoodMovement(sessionId, request.toGoodMovementData())
            if (result is ResultOf.Failure) {
                logger.warn("Failed to create good movement: ${result.message}")
                return CreateGoodMovementResponse.Failure(
                    errorType = GoodMovementError.WMS_VALIDATION_ERROR,
                    errorMessage = result.message
                )
            }

            lateinit var goodMovementData: GoodMovementData
            Database.connect(dataSource)
            transaction {
                addLogger(exposedLogger)
                val dao = GoodMovementDao.find { GoodMovement.userId eq userId.toInt() }
                    .orderBy(GoodMovement.id to SortOrder.DESC)
                    .limit(1)
                    .first()

                goodMovementData  = goodMovementService.getGoodMovementData(dao)
            }
            return CreateGoodMovementResponse.Success(goodMovementData)
        } finally {
            itemLockService.unlock(userId)
        }
    }
}