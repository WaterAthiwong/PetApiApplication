package com.champaca.inventorydata.conditionroom.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.conditionroom.ConditionRoomService
import com.champaca.inventorydata.databasetable.GoodMovement
import com.champaca.inventorydata.databasetable.dao.GoodMovementDao
import com.champaca.inventorydata.databasetable.dao.GoodMovementDao.Companion.TRANSFER_TO_CONDITION_ROOM
import com.champaca.inventorydata.pile.model.MovingItem
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.json.contains
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.sql.DataSource

@Service
class GetStuckInTransferItemsUseCase(
    val dataSource: DataSource,
    val conditionRoomService: ConditionRoomService
) {
    val logger = LoggerFactory.getLogger(GetStuckInTransferItemsUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(): List<MovingItem> {
        Database.connect(dataSource)

        var remainingItems = listOf<MovingItem>()
        transaction {
            addLogger(exposedLogger)

            val goodMovementIds = findTransferToConditionRoomGoodMovements()
            remainingItems = conditionRoomService.getRemainingItems(goodMovementIds)
        }
        return remainingItems
    }

    private fun findTransferToConditionRoomGoodMovements(): List<Int> {
        return GoodMovementDao.find {
            (GoodMovement.extraAttributes.contains("{\"purpose\":\"${TRANSFER_TO_CONDITION_ROOM}\"}")) and
            (GoodMovement.status eq "A") and (GoodMovement.productionDate greaterEq LocalDate.now().minusMonths(1))
        }.map { it.id.value }
    }
}