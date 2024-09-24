package com.champaca.inventorydata.conditionroom.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.conditionroom.ConditionRoomService
import com.champaca.inventorydata.conditionroom.model.SuggestedShelf
import com.champaca.inventorydata.conditionroom.response.GetRemainingItemsResponse
import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.request.PileItem
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetRemainingItemsUseCase(
    val dataSource: DataSource,
    val conditionRoomService: ConditionRoomService
) {
    val logger = LoggerFactory.getLogger(GetRemainingItemsUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(goodMovementId: Int): GetRemainingItemsResponse {
        Database.connect(dataSource)

        var remainingItems = listOf<MovingItem>()
        var suggestedShelves = listOf<SuggestedShelf>()
        transaction {
            addLogger(exposedLogger)
            remainingItems = conditionRoomService.getRemainingItems(goodMovementId)
            suggestedShelves = conditionRoomService.getSuggestedShelves(remainingItems)
        }

        val results = remainingItems.groupBy { it.matCode }.mapValues { (matCode, items) ->
            val first = items.first()
            PileItem(
                skuId = first.skuId,
                matCode = first.matCode,
                qty = items.sumOf { it.qty },
                lotRefCode = ""
            )
        }
            .values.toList()
        return GetRemainingItemsResponse.Success(results, suggestedShelves)
    }
}