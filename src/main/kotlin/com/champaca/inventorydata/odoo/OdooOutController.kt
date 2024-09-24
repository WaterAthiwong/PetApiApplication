package com.champaca.inventorydata.odoo

import com.champaca.inventorydata.odoo.model.out.CreatePurchaseOrderRequest
import com.champaca.inventorydata.odoo.model.out.CreateStockPickingRequest
import com.champaca.inventorydata.odoo.model.out.SalesOrderSendDeliveryRequest
import com.champaca.inventorydata.odoo.request.out.DeliveryOrderRequest
import com.champaca.inventorydata.odoo.request.out.LogPurchaseOrderRequest
import com.champaca.inventorydata.odoo.request.out.SawnTimberPurchaseOrderRequest
import com.champaca.inventorydata.odoo.request.out.StockMovementRequest
import com.champaca.inventorydata.odoo.usecase.out.DeliveryOrderUseCase
import com.champaca.inventorydata.odoo.usecase.out.LogPurchaseOrderUseCase
import com.champaca.inventorydata.odoo.usecase.out.SawnTimberPurchaseOrderUseCase
import com.champaca.inventorydata.odoo.usecase.out.StockMovementUseCase
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/odoo/out")
@RestController
@CrossOrigin(origins = ["*"])
class OdooOutController(
    val logPurchaseOrderUseCase: LogPurchaseOrderUseCase,
    val sawnTimberPurchaseOrderUseCase: SawnTimberPurchaseOrderUseCase,
    val stockMovementUseCase: StockMovementUseCase,
    val deliveryOrderUseCase: DeliveryOrderUseCase
) {

    @PostMapping("/po/log/send")
    fun sendLogPurchaseOrderToOdoo(@RequestBody request: LogPurchaseOrderRequest): List<CreatePurchaseOrderRequest> {
        return logPurchaseOrderUseCase.sendToOdoo(request)
    }

    @PostMapping("/po/plywood/send")
    fun sendPlywoodPurchaseOrderToOdoo(@RequestBody request: SawnTimberPurchaseOrderRequest): List<CreatePurchaseOrderRequest> {
        return sawnTimberPurchaseOrderUseCase.sendToOdoo(request)
    }

    @PostMapping("/stockMovement/send")
    fun sendStockMovementToOdoo(@RequestBody request: StockMovementRequest): List<CreateStockPickingRequest> {
        return stockMovementUseCase.sendToOdoo(request)
    }

    @PostMapping("/salesOrder/deliver")
    fun deliveryOrderToOdoo(@RequestBody request: DeliveryOrderRequest): Map<Int, SalesOrderSendDeliveryRequest> {
        return deliveryOrderUseCase.sendToOdoo(request)
    }
}