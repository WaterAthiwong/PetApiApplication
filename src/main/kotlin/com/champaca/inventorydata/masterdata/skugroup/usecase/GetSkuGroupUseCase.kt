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
class GetSkuGroupUseCase(
    val dataSource: DataSource,
    val skuGroupRepository: SkuGroupRepository
) {

    val skuGroupForProcess: Map<String, List<String>> = mapOf(
        "SM" to listOf("R0", "R1", "R3"),
        "DK" to listOf("R1", "R2", "R4", "R9"),
        "WH" to listOf("R0", "R1", "R2", "R3", "R4", "R5", "R6", "R7", "R9", "RC", "L0"),
        "RC" to listOf("R0", "R1", "R5", "R3"),
        "RM" to listOf("L0", "L3", "L4", "L6", "L8", "L9", "LA", "R1", "R2"),
        "PR" to listOf("L0", "L3", "L4", "L6", "L8", "L9", "LA", "R1", "R2"),
        "SD" to listOf("L0", "L3", "L4", "L6", "L8", "L9", "LA", "R1", "R2"),
        "FN" to listOf("L0", "L3", "L4", "L6", "L8", "L9", "LA", "R1", "R2")
    )

    val logger = LoggerFactory.getLogger(GetSkuGroupUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(processPrefix: String?): List<SkuGroupData> {
        Database.connect(dataSource)

        var results: List<SkuGroupData> = listOf()
        transaction {
            addLogger(exposedLogger)
            results = skuGroupRepository.getAll().map {
                SkuGroupData(code = it.erpGroupCode, name = "${it.erpGroupCode} ${it.erpGroupName}")
            }

            if (skuGroupForProcess.containsKey(processPrefix)) {
                val skuGroupCodes = skuGroupForProcess[processPrefix]!!
                results = results.filter { skuGroupCodes.contains(it.code)  }
            }
        }
        return results
    }
}