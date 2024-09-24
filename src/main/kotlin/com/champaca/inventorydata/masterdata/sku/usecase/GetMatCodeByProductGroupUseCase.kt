package com.champaca.inventorydata.masterdata.sku.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.masterdata.sku.SkuRepository
import com.champaca.inventorydata.masterdata.sku.response.MatCodeAndSkuId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetMatCodeByProductGroupUseCase(
    val dataSource: DataSource,
    val skuRepository: SkuRepository
) {

    val logger = LoggerFactory.getLogger(GetMatCodeByProductGroupUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(skuGroup: String): List<MatCodeAndSkuId> {
        Database.connect(dataSource)

        var results = listOf<MatCodeAndSkuId>()
        transaction {
            addLogger(exposedLogger)
            results = skuRepository.findBySkuGroupCode(skuGroup).map { MatCodeAndSkuId(it.matCode, it.id.value)}
        }
        return results
    }
}