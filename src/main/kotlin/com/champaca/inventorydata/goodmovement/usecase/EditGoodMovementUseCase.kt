package com.champaca.inventorydata.goodmovement.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.dao.GoodMovementDao
import com.champaca.inventorydata.goodmovement.GoodMovementError
import com.champaca.inventorydata.goodmovement.GoodMovementService
import com.champaca.inventorydata.goodmovement.model.GoodMovementData
import com.champaca.inventorydata.goodmovement.request.CreateGoodMovementRequest
import com.champaca.inventorydata.goodmovement.response.CreateGoodMovementResponse
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class EditGoodMovementUseCase(
    val dataSource: DataSource,
    val wmsService: WmsService,
    val goodMovementService: GoodMovementService
) {
    val logger = LoggerFactory.getLogger(EditGoodMovementUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(session: String, userId: String, request: CreateGoodMovementRequest): CreateGoodMovementResponse {
        val result = wmsService.editGoodMovement(session, request.toGoodMovementData())
        if (result is ResultOf.Failure) {
            logger.warn("Failed to edit good movement ${request.id}: ${result.message}")
            return CreateGoodMovementResponse.Failure(
                errorType = GoodMovementError.WMS_VALIDATION_ERROR,
                errorMessage = result.message
            )
        }

        var goodMovement: GoodMovementData? = null
        Database.connect(dataSource)
        transaction {
            addLogger(exposedLogger)
            val dao = GoodMovementDao.findById(request.id!!)!!
            goodMovement  = goodMovementService.getGoodMovementData(dao)
        }
        return CreateGoodMovementResponse.Success(goodMovement!!)
    }
}