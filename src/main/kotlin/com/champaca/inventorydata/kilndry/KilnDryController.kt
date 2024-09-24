package com.champaca.inventorydata.kilndry

import com.champaca.inventorydata.kilndry.request.GetStockRequest
import com.champaca.inventorydata.model.ProcessedWood
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/kilnDry")
@RestController
@CrossOrigin(origins = ["*"])
class KilnDryController(val kilnDryService: KilnDryService) {

    @PostMapping(
        value = ["/stock"], consumes = ["application/json"], produces = ["application/json"])
    fun getStock(@RequestBody request: GetStockRequest): List<ProcessedWood> {
        return kilnDryService.getStock(request)
    }
}