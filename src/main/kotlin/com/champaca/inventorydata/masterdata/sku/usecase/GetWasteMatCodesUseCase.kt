package com.champaca.inventorydata.masterdata.sku.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.dao.SkuDao
import com.champaca.inventorydata.masterdata.sku.SkuRepository
import com.champaca.inventorydata.masterdata.sku.model.MatCodeQuery
import com.champaca.inventorydata.masterdata.sku.response.CheckMatCodeExistResponse
import com.champaca.inventorydata.masterdata.sku.response.MatCodeAndSkuId
import com.champaca.inventorydata.masterdata.skugroup.SkuGroupRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetWasteMatCodesUseCase(
    val dataSource: DataSource,
    val skuRepository: SkuRepository
) {
    companion object {
        const val WASTE_CODE = "W0"
    }

    val logger = LoggerFactory.getLogger(GetWasteMatCodesUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(): List<MatCodeAndSkuId> {
        Database.connect(dataSource)
        var results = listOf<MatCodeAndSkuId>()
        transaction {
            addLogger(exposedLogger)
            results = skuRepository.findBySkuGroupCode(WASTE_CODE).map {
                MatCodeAndSkuId(it.matCode, it.id.value)
            }
        }
        return results
    }
}