package com.champaca.inventorydata.masterdata.skugroup.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.masterdata.skugroup.SkuGroupRepository
import com.champaca.inventorydata.masterdata.skugroup.response.SkuGroupData
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetSkuMainGroupUseCase{

    val skuGroupForProcess: Map<String, List<String>> = mapOf(
        "SM" to listOf("RM"),
        "DK" to listOf("RM"),
        "WH" to listOf("RM", "COMPONENT"),
        "RC" to listOf("RM"),
        "RM" to listOf("RM", "COMPONENT", "HFG", "TFG", "FG"),
        "PR" to listOf("COMPONENT", "HFG", "TFG", "FG"),
        "SD" to listOf("COMPONENT", "HFG", "TFG", "FG"),
        "FN" to listOf("HFG", "TFG", "FG")
    )

    val logger = LoggerFactory.getLogger(GetSkuMainGroupUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(processPrefix: String?): List<String> {
        if (skuGroupForProcess.containsKey(processPrefix)) {
            return skuGroupForProcess[processPrefix]!!
        }
        return listOf()
    }
}