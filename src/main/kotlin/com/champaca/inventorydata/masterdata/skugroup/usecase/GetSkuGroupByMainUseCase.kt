package com.champaca.inventorydata.masterdata.skugroup.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.masterdata.skugroup.SkuGroupRepository
import com.champaca.inventorydata.masterdata.skugroup.response.SkuGroupData
import com.champaca.inventorydata.masterdata.skugroup.response.SkuGroupMainData
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetSkuGroupByMainUseCase(
    val dataSource: DataSource,
    val skuGroupRepository: SkuGroupRepository,
) {
    val logger = LoggerFactory.getLogger(GetSkuGroupByMainUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(mainGroup: String?): List<SkuGroupMainData> {
        Database.connect(dataSource)

        var results: List<SkuGroupMainData> = listOf()
        transaction {
            addLogger(exposedLogger)
            results = skuGroupRepository.getAll().map {
                SkuGroupMainData(code = it.erpGroupCode, name = "${it.erpGroupCode} ${it.erpGroupName}" , mainGroup = it.erpMainGroupName)
            }
            println(results)
            println(mainGroup)
            if (mainGroup != null) {
                results = results.filter { it.mainGroup == mainGroup }
            }
        }
        return results
    }
}