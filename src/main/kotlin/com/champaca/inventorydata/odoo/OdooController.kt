package com.champaca.inventorydata.odoo

import com.champaca.inventorydata.odoo.request.UpsertPartnerRequest
import com.champaca.inventorydata.odoo.request.UpsertSalesOrderRequest
import com.champaca.inventorydata.odoo.request.UpsertSkuRequest
import com.champaca.inventorydata.odoo.usecase.*
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/odoo")
@RestController
@CrossOrigin(origins = ["*"])
class OdooController(
    val createPartnerUseCase: CreatePartnerUseCase,
    val updatePartnerUseCase: UpdatePartnerUseCase,
    val createSalesOrderUseCase: CreateSalesOrderUseCase,
    val updateSalesOrderUseCase: UpdateSalesOrderUseCase,
    val upsertSkuUseCase: UpsertSkuUseCase
) {
    @PostMapping("products")
    fun createSku(@RequestBody request: UpsertSkuRequest) {
        upsertSkuUseCase.create(request)
    }

    @PutMapping("products/{id}")
    fun updateSku(@PathVariable("id") id: Int, @RequestBody request: UpsertSkuRequest) {
        request.odooId = id
        upsertSkuUseCase.update(request)
    }

    @PostMapping("sale_orders")
    fun createSalesOrder(@RequestBody request: UpsertSalesOrderRequest) {
        createSalesOrderUseCase.execute(request)
    }

    @PutMapping("sale_orders/{id}")
    fun updateSalesOrder(@PathVariable("id") id: Int, @RequestBody request: UpsertSalesOrderRequest) {
        request.odooId = id
        updateSalesOrderUseCase.execute(request)
    }

    @PostMapping("partner")
    fun createPartner(@RequestBody request: UpsertPartnerRequest) {
        createPartnerUseCase.execute(request)
    }

    @PutMapping("partner/{id}")
    fun updatePartner(@PathVariable("id") id: Int, @RequestBody request: UpsertPartnerRequest) {
        request.odooId = id
        updatePartnerUseCase.execute(request)
    }
}