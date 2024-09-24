package com.champaca.inventorydata.goodmovement.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.GoodMovement
import com.champaca.inventorydata.databasetable.dao.GoodMovementDao
import com.champaca.inventorydata.databasetable.dao.ManufacturingLineDao
import com.champaca.inventorydata.goodmovement.GoodMovementError
import com.champaca.inventorydata.goodmovement.GoodMovementService
import com.champaca.inventorydata.goodmovement.model.GoodMovementData
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.goodmovement.response.CreateGoodMovementResponse
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.sql.DataSource


@Service
class CreateMatchingGoodReceiptUseCase(
    val dataSource: DataSource,
    val wmsService: WmsService,
    val goodMovementService: GoodMovementService,
    val itemLockService: ItemLockService
) {
    val logger = LoggerFactory.getLogger(CreateMatchingGoodReceiptUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)
    fun execute(sessionId: String, userId: String, pickingGoodMovementId: Int): CreateGoodMovementResponse {
        try {
            itemLockService.lock(userId)
            Database.connect(dataSource)

            var errorType = GoodMovementError.NONE
            var errorMessage = ""
            lateinit var pickingGoodMovement: GoodMovementDao
            var manufacturingLine: ManufacturingLineDao? = null
            transaction {
                addLogger(exposedLogger)

                val tempPickingGoodMovement = GoodMovementDao.findById(pickingGoodMovementId)
                if (tempPickingGoodMovement == null) {
                    errorType = GoodMovementError.GODD_MOVEMENT_NOT_FOUND
                    errorMessage = "Good movement not found"
                    logger.warn("Good movement ${pickingGoodMovementId} not found")
                    return@transaction
                }
                pickingGoodMovement = tempPickingGoodMovement

                if (pickingGoodMovement.jobNo.isNullOrEmpty() && pickingGoodMovement.orderNo.isNullOrEmpty()) {
                    errorType = GoodMovementError.INVALID_INPUT
                    errorMessage = "JobNo and OrderNo cannot be empty at the same time"
                    logger.warn("Good movement ${pickingGoodMovementId} can not have empty JobNo and OrderNo at the same time")
                    return@transaction
                }

                val matchingGoodMovement = findMatchingGoodMovement(pickingGoodMovement)
                if (matchingGoodMovement != null) {
                    errorType = GoodMovementError.MATCHING_GOOD_MOVEMENT_ALREADY_EXISTS
                    errorMessage = "Matching good movement already exists"
                    logger.warn("Matching good movement already exists for jobNo: ${pickingGoodMovement.jobNo} and/or orderNo: ${pickingGoodMovement.orderNo}")
                    return@transaction
                }

                manufacturingLine = ManufacturingLineDao.findById(pickingGoodMovement.manufacturingLineId!!)
            }

            if (errorType != GoodMovementError.NONE) {
                return CreateGoodMovementResponse.Failure(errorType, errorMessage)
            }

            val goodMovementData = GoodMovementData(
                id = 0,
                code = "",
                type = GoodMovementType.GOODS_RECEIPT,
                processType = "",
                processTypeId = manufacturingLine!!.processTypeId,
                manufacturingLine = "",
                manufacturingLineId = pickingGoodMovement.manufacturingLineId!!,
                departmentId = pickingGoodMovement.departmentId.value,
                department = "",
                productionDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                orderNo = pickingGoodMovement.orderNo,
                jobNo = pickingGoodMovement.jobNo,
                poNo = pickingGoodMovement.poNo,
                invoiceNo = pickingGoodMovement.invoiceNo,
                lotNo = pickingGoodMovement.lotNo,
                supplierId = pickingGoodMovement.supplierId,
                supplier = null,
                createdBy = "",
                approvedBy = null,
                remark = pickingGoodMovement.remark,
                extraAttributes = null,
                productType = pickingGoodMovement.productType
            )
            val createResult = wmsService.createGoodMovement(sessionId, goodMovementData)
            if (createResult is ResultOf.Failure) {
                errorType = GoodMovementError.WMS_VALIDATION_ERROR
                errorMessage = createResult.message!!
                logger.warn("WMS validation error: ${createResult.message}")
                return CreateGoodMovementResponse.Failure(errorType, errorMessage)
            }

            var receivingGoodMovementId = -1
            lateinit var receivingGoodMovementData: GoodMovementData
            transaction {
                addLogger(exposedLogger)

                val receivingGoodMovement = findMatchingGoodMovement(pickingGoodMovement)
                receivingGoodMovementId = receivingGoodMovement!!.id.value
                receivingGoodMovementData = goodMovementService.getGoodMovementData(receivingGoodMovement)
            }

            val pairResult =
                wmsService.addReferenceGoodMovement(sessionId, receivingGoodMovementId, listOf(pickingGoodMovementId))
            if (pairResult is ResultOf.Failure) {
                errorType = GoodMovementError.WMS_VALIDATION_ERROR
                errorMessage = pairResult.message!!
                logger.warn("WMS validation error: ${pairResult.message}")
                return CreateGoodMovementResponse.Failure(errorType, errorMessage)
            }

            return CreateGoodMovementResponse.Success(receivingGoodMovementData)
        } finally {
            itemLockService.unlock(userId)
        }
    }

    private fun findMatchingGoodMovement(pickingGoodMovement: GoodMovementDao): GoodMovementDao? {
        val query = GoodMovement.selectAll().where { (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) and
                (GoodMovement.manufacturingLineId eq pickingGoodMovement.manufacturingLineId) and
                (GoodMovement.status eq "A")}

        if (!pickingGoodMovement.jobNo.isNullOrEmpty()) {
            query.andWhere { GoodMovement.jobNo eq pickingGoodMovement.jobNo }
        }

        if (!pickingGoodMovement.orderNo.isNullOrEmpty()) {
            query.andWhere { GoodMovement.orderNo eq pickingGoodMovement.orderNo }
        }

        return GoodMovementDao.wrapRows(query).firstOrNull()
    }
}