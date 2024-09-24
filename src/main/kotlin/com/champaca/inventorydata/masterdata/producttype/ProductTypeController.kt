package com.champaca.inventorydata.masterdata.producttype

import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/productType")
@RestController
@CrossOrigin(origins = ["*"])
class ProductTypeController {

    val productTypes = listOf("ไม้ซุง", "ไม้ SLAB", "ไม้แปรรูป", "เศษ", "COMPONENT", "HFG", "TFG", "FG เฟอร์นิเจอร์", "FG ไม้พื้น", "FG ประตูบันได")

    @GetMapping("/")
    fun getAllProductTypes(): List<String> {
        return productTypes
    }
}