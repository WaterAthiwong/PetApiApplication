package com.champaca.inventorydata.data.stock

import com.champaca.inventorydata.data.stock.request.StockInProcessRequest
import com.champaca.inventorydata.data.stock.request.StockInStorageRequest
import com.champaca.inventorydata.data.stock.response.StockResponse
import com.champaca.inventorydata.data.stock.usecase.GetStockInProcessUseCase
import com.champaca.inventorydata.data.stock.usecase.GetStockInStorageUseCase
import org.springframework.web.bind.annotation.*

@RequestMapping("/stock")
@RestController
@CrossOrigin(origins = ["*"])
class StockController(
    val getStockInStorageUseCase: GetStockInStorageUseCase,
    val getStockInProcessUseCase: GetStockInProcessUseCase
) {

    @PostMapping("/")
    fun getStockInStorage(@RequestBody request: StockInStorageRequest): StockResponse {
        return getStockInStorageUseCase.execute(request)
    }

    @PostMapping("/process")
    fun getStockInProcess(@RequestBody request: StockInProcessRequest): StockResponse {
        return getStockInProcessUseCase.execute(request)
    }
}