package com.champaca.inventorydata.cleansing.usecase

import com.champaca.inventorydata.log.usecase.ReceiveLogUseCase
import org.springframework.stereotype.Service

@Service
class ReceiveMultipleLogs(
    val receiveLogUseCase: ReceiveLogUseCase
) {
    fun execute(username: String, refCodes: List<String>) {
        refCodes.forEach {
            val request = com.champaca.inventorydata.log.request.ReceiveLogRequest(
                barcode = it,
                receivedDate = "2024-03-09",
                forestryBook = null,
                forestryBookNo = null,
                logDeliveryId = null,
                location = null
            )
            val result = receiveLogUseCase.execute(username, request)
            if (result is com.champaca.inventorydata.log.response.ReceivedLogResponse.Success) {
                println("Received log for $it")
            } else {
                println("Failed to receive log for $it")
            }
        }
    }
}