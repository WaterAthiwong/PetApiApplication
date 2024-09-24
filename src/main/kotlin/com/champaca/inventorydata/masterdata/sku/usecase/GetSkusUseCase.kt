package com.champaca.inventorydata.masterdata.sku.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.Sku
import com.champaca.inventorydata.masterdata.sku.model.SkuData
import com.champaca.inventorydata.masterdata.sku.request.GetSkusRequest
import com.champaca.inventorydata.masterdata.skugroup.SkuGroupRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import javax.sql.DataSource

@Service
class GetSkusUseCase(
    val dataSource: DataSource,
    val skuGroupRepository: SkuGroupRepository
) {
    val logger = LoggerFactory.getLogger(GetSkusUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: GetSkusRequest): List<SkuData> {
        Database.connect(dataSource)

        var skuDatas = listOf<SkuData>()
        transaction {
            addLogger(exposedLogger)
            skuDatas = getSkus(request)
        }
        return skuDatas
    }

    private fun getSkus(request: GetSkusRequest): List<SkuData> {
        val query = Sku.selectAll()
            .where { (Sku.matCode like "${request.matCodePattern}%") and (Sku.status eq "A") }
            .limit(10000)
        val skuGroups = skuGroupRepository.getAll().associateBy { it.id.value }
        return query.map { resultRow ->
            val skuGroup = skuGroups[resultRow[Sku.skuGroupId]]!!
            SkuData(
                skuGroupId = resultRow[Sku.skuGroupId],
                skuGroupName = skuGroup.erpGroupName,
                code = resultRow[Sku.code],
                matCode = resultRow[Sku.matCode],
                name = resultRow[Sku.name],
                thickness = resultRow[Sku.thickness],
                width = resultRow[Sku.width],
                length = resultRow[Sku.length],
                widthUom = resultRow[Sku.widthUom],
                lengthUom = resultRow[Sku.lengthUom],
                thicknessUom = resultRow[Sku.thicknessUom],
                circumference = resultRow[Sku.circumference],
                circumferenceUom = resultRow[Sku.circumferenceUom],
                volumnM3 = resultRow[Sku.volumnM3],
                volumnFt3 = resultRow[Sku.volumnFt3],
                areaM2 = resultRow[Sku.areaM2] ?: BigDecimal.ZERO,
                species = resultRow[Sku.species],
                grade = resultRow[Sku.grade],
                fsc = if(resultRow[Sku.fsc]) "FSC" else "NON FSC"
            )
        }
    }
}