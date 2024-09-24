package com.champaca.inventorydata.masterdata.sku.usecase

import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.dao.SkuDao
import com.champaca.inventorydata.masterdata.sku.SkuRepository
import com.champaca.inventorydata.masterdata.sku.fg.FinishedGoodMatCodeComposer
import com.champaca.inventorydata.masterdata.sku.fg.FinishedGoodType
import com.champaca.inventorydata.masterdata.sku.model.FinishedGoodMatCodeQuery
import com.champaca.inventorydata.masterdata.sku.response.CheckMatCodeExistResponse
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class CheckFinishedGoodMatCodeExistUseCase(
    val dataSource: DataSource,
    val skuRepository: SkuRepository,
    val fgMatCodeComposers: Map<FinishedGoodType, FinishedGoodMatCodeComposer>
) {
    val logger = LoggerFactory.getLogger(CheckFinishedGoodMatCodeExistUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(query: FinishedGoodMatCodeQuery): CheckMatCodeExistResponse {
        Database.connect(dataSource)

        val type = FinishedGoodType.fromOdooName(query.type)
        val matCode = fgMatCodeComposers.get(type)?.compose(query) ?: ""
        if (matCode == "") {
            return CheckMatCodeExistResponse(isValid = false)
        }
        logger.info( "Mat Code: $matCode")
        var sku: SkuDao? = null
        transaction {
            addLogger(exposedLogger)
            sku = skuRepository.findByMatCode(matCode)
        }
        if (sku == null) {
            return CheckMatCodeExistResponse(isValid = false)
        }

        return CheckMatCodeExistResponse(isValid = true, matCode = matCode, skuId = sku!!.id.value)
    }
}