package com.champaca.inventorydata.conditionroom.usecase

import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ItemLockService
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.request.PickPileRequest
import com.champaca.inventorydata.pile.response.PickPileResponse
import com.champaca.inventorydata.pile.usecase.PickPileUseCase
import com.champaca.inventorydata.pile.usecase.toPickedGoodMovement
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class AccumulatePileUseCase(
    val dataSource: DataSource,
    val pickPileUseCase: PickPileUseCase,
    val itemLockService: ItemLockService
) {

    val logger = LoggerFactory.getLogger(AccumulatePileUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    // ที่ต้องทำแยกออกมาเป็นกรณีนี้ แทนที่จะให้ไปเรียก /pile/wms/pick เพราะว่าต้องการล็อกสองชั้นคือ ล็อกที่ใบเบิก กับล็อกที่กองไม้ด้วย กันพลาดว่าเกิด
    // race condition ซึ่งจะซวยกว่าการเบิกกองไม้ของโรงเลื่อย, เตาอบ, คลังแปรรูป
    fun execute(sessionId: String, userId: String, request: PickPileRequest): PickPileResponse {
        val lockName = "goodMovement${request.goodMovementId}"
        try {
            itemLockService.lock(lockName)   // Lock this good movement to ensure the transferred_item table correctness.
            itemLockService.lock(request.pileCode)   // Lock the pile as well

            val dataHolder = pickPileUseCase.pick(sessionId, userId, request)
            if (dataHolder.hasError()) {
                return PickPileResponse.Failure(
                    errorType = dataHolder.errorType,
                    errorMessage = dataHolder.errorMessage,
                    goodMovement = dataHolder.goodMovement?.toPickedGoodMovement(),
                    items = dataHolder.pickedItems
                )
            }

            return PickPileResponse.Success(
                goodMovement = dataHolder.goodMovement!!.toPickedGoodMovement(),
                items = dataHolder.pickedItems,
                pileCount = dataHolder.pileCount,
                itemCount = dataHolder.itemCount
            )
        } finally {
            itemLockService.unlock(request.pileCode) // Unlock the pile
            itemLockService.unlock(lockName)   // Unlock the good movement
        }
    }
}