package com.champaca.inventorydata.goodmovement.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.GoodMovement
import com.champaca.inventorydata.databasetable.dao.GoodMovementDao
import com.champaca.inventorydata.goodmovement.GoodMovementError
import com.champaca.inventorydata.goodmovement.GoodMovementService
import com.champaca.inventorydata.goodmovement.model.GoodMovementData
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.goodmovement.model.ReferencedGoodMovement
import com.champaca.inventorydata.goodmovement.response.GetGoodMovementDataResponse
import com.champaca.inventorydata.masterdata.user.UserRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetGoodMovementDataUseCase(
    val dataSource: DataSource,
    val goodMovementService: GoodMovementService,
    val userRepository: UserRepository
) {
    val logger = LoggerFactory.getLogger(GetGoodMovementDataUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(userId: String, goodMovementId: Int): GetGoodMovementDataResponse {
        Database.connect(dataSource)

        var goodMovementData: GoodMovementData? = null
        var error = GoodMovementError.NONE
        var canApprove = false
        var canCreateMatchingGoodReceipt = false
        var referencedGoodMovements = emptyList<ReferencedGoodMovement>()
        transaction {
            addLogger(exposedLogger)
            val dao = GoodMovementDao.findById(goodMovementId)
            if (dao == null) {
                error = GoodMovementError.GODD_MOVEMENT_NOT_FOUND
                return@transaction
            }
            goodMovementData = goodMovementService.getGoodMovementData(dao)
            canApprove = canTheUserApprove(userId, goodMovementData!!)
            canCreateMatchingGoodReceipt = goodMovementData!!.type == GoodMovementType.PICKING_ORDER &&
                    dao.manufacturingLineId != null && dao.goodReceiptGoodMovementId == null

            if (goodMovementData!!.type == GoodMovementType.GOODS_RECEIPT) {
                referencedGoodMovements = findReferencedGoodMovements(goodMovementId)
            }
        }
        if (error == GoodMovementError.NONE) {
            return GetGoodMovementDataResponse.Success(
                goodMovement = goodMovementData!!,
                canApprove =  canApprove,
                canCreateMatchingGoodReceipt =  canCreateMatchingGoodReceipt,
                referencedGoodMovements =  referencedGoodMovements)
        } else {
            return GetGoodMovementDataResponse.Failure(error)
        }
    }

    private fun canTheUserApprove(userId: String, goodMovementData: GoodMovementData): Boolean {
        if (goodMovementData.type == GoodMovementType.GOODS_RECEIPT) {
            return true
        } else {
            if (goodMovementData.processTypeId == null) {
                // This is ใบเบิกทั่วไป (General Issue) so retrun true for now
                return true
            } else {
                // This is ใบเบิกสำหรับการผลิต (Issue for Production) so we need to check if the user has permission to approve
                return userRepository.checkUserHasProcessApprovalPermission(userId.toInt(), goodMovementData.processTypeId!!)
            }
        }
    }

    private fun findReferencedGoodMovements(goodMovementId: Int): List<ReferencedGoodMovement> {
        return GoodMovementDao.find { GoodMovement.goodReceiptGoodMovementId eq goodMovementId }
            .map {
                ReferencedGoodMovement(
                    id = it.id.value,
                    code = it.code,
                    jobNo = it.jobNo,
                )
            }
    }
}