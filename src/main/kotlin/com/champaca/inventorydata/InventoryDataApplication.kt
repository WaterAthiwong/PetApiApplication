package com.champaca.inventorydata

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import java.util.TimeZone
import javax.annotation.PostConstruct

@SpringBootApplication
@EnableCaching
class InventoryDataApplication {

    @PostConstruct
    fun setup() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Bangkok"))
    }
}

fun main(args: Array<String>) {
    runApplication<InventoryDataApplication>(*args)
}