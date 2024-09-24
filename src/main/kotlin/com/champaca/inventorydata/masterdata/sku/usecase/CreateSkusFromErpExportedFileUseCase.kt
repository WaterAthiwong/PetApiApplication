package com.champaca.inventorydata.masterdata.sku.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.masterdata.sku.SkuRepository
import com.champaca.inventorydata.masterdata.sku.excel.ErpExportReader
import com.champaca.inventorydata.masterdata.sku.model.SkuData
import com.champaca.inventorydata.masterdata.skugroup.SkuGroupRepository
import com.champaca.inventorydata.utils.DateTimeUtil
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import javax.sql.DataSource

@Service
class CreateSkusFromErpExportedFileUseCase(
    val datasource: DataSource,
    val erpExportReader: ErpExportReader,
    val skuRepository: SkuRepository,
    val skuGroupRepository: SkuGroupRepository,
    val dateTimeUtil: DateTimeUtil
) {

    @Value("\${champaca.uploads.location}")
    lateinit var uploadRoot: String

    fun execute(file: MultipartFile): List<SkuData> {
        val uploadRootPath = Paths.get(uploadRoot)
        Files.createDirectories(uploadRootPath)
        val fileName = "erpExportedSku_${dateTimeUtil.getCurrentDateTimeString("yyyy_MM_dd_HH_mm")}.xlsx"
        Files.copy(file.inputStream, uploadRootPath.resolve(fileName))

        var skuGroupIds: Map<String, Int> = mapOf()
        Database.connect(datasource)
        transaction {
            addLogger(ExposedInfoLogger)

            skuGroupIds = skuGroupRepository.getAll().associateBy({ it.erpGroupCode }, { it.id.value })
        }
        val skuDatas = erpExportReader.readFile("${uploadRoot}/${fileName}", skuGroupIds)
        var toBeAddedSkus = getSkusToBeAdded(skuDatas)
        return toBeAddedSkus
    }

    private fun getSkusToBeAdded(skuDatas: List<SkuData>): List<SkuData> {
        var toBeAddedSkus: List<SkuData> = listOf()
        val matCodes = skuDatas.map { it.matCode }
        transaction {
            addLogger(ExposedInfoLogger)
            val nonExistingMatCodes = skuRepository.findNonExistingMatCodes(matCodes)
            toBeAddedSkus = skuDatas.filter { nonExistingMatCodes.contains(it.matCode) }

            val invalidUoms = toBeAddedSkus.filter { it.thicknessUom == "NA" && it.widthUom == "NA" && it.lengthUom == "NA" }
            toBeAddedSkus = toBeAddedSkus.filter { !invalidUoms.contains(it) }
        }
        return toBeAddedSkus
    }
}