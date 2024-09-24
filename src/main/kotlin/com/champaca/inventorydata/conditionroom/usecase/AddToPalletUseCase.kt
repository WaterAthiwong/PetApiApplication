package com.champaca.inventorydata.conditionroom.usecase

import com.champaca.inventorydata.conditionroom.request.AddToPalletRequest
import com.champaca.inventorydata.conditionroom.response.AddToPalletResponse
import org.springframework.stereotype.Service

@Service
class AddToPalletUseCase {
    fun execute(request: AddToPalletRequest): AddToPalletResponse {
        return AddToPalletResponse.Success("pileCode")
    }
}