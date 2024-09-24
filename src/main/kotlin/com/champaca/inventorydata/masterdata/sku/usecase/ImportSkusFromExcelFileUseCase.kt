package com.champaca.inventorydata.masterdata.sku.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.Sku
import com.champaca.inventorydata.masterdata.sku.SkuRepository
import com.champaca.inventorydata.masterdata.sku.excel.ExcelReader
import com.champaca.inventorydata.masterdata.sku.excel.FinishedGoodsExcelReader
import com.champaca.inventorydata.masterdata.sku.model.SkuData
import com.champaca.inventorydata.masterdata.sku.response.ImportSkusFromExcelFileResponse
import com.champaca.inventorydata.masterdata.skugroup.SkuGroupRepository
import com.champaca.inventorydata.utils.DateTimeUtil
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class ImportSkusFromExcelFileUseCase(
    val datasource: DataSource,
    val excelReader: ExcelReader,
    val finishedGoodsExcelReader: FinishedGoodsExcelReader,
    val skuRepository: SkuRepository,
    val skuGroupRepository: SkuGroupRepository,
    val dateTimeUtil: DateTimeUtil,
    val cacheManager: CacheManager
) {

    @Value("\${champaca.uploads.location}")
    lateinit var uploadRoot: String

    val logger = LoggerFactory.getLogger(ImportSkusFromExcelFileUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(file: MultipartFile, isFinishedGoodsSkus: Boolean = false): ImportSkusFromExcelFileResponse {
        val uploadRootPath = Paths.get(uploadRoot)
        Files.createDirectories(uploadRootPath)
        val fileName = "ExcelSku_${dateTimeUtil.getCurrentDateTimeString("yyyy_MM_dd_HH_mm_ss")}.xlsx"
        Files.copy(file.inputStream, uploadRootPath.resolve(fileName))

        var skuGroupIds: Map<String, Int> = mapOf()
        Database.connect(datasource)
        transaction {
            addLogger(exposedLogger)
            skuGroupIds = skuGroupRepository.getAll().associateBy({ "${it.erpMainGroupId}${it.erpGroupCode}" }, { it.id.value })
        }
        val skuDatas = if (isFinishedGoodsSkus) {
            finishedGoodsExcelReader.readFile("${uploadRoot}/${fileName}", skuGroupIds)
        } else {
            excelReader.readFile("${uploadRoot}/${fileName}", skuGroupIds)
        }
        var toBeAddedSkus = listOf<SkuData>()
        transaction {
            addLogger(exposedLogger)
            toBeAddedSkus = getSkusToBeAdded(skuDatas)
            if (toBeAddedSkus.isNotEmpty()) {
                insertSku(toBeAddedSkus)
            }
        }
        // ลบ cache ที่มี key เป็น matCode ของ sku ที่ถูกเพิ่มเข้ามา
        toBeAddedSkus.forEach { sku -> cacheManager.getCache("skusByMatCode")?.evict(sku.matCode) }
        return ImportSkusFromExcelFileResponse.Success(
            existings = skuDatas - toBeAddedSkus,
            createds = toBeAddedSkus
        )
    }

    private fun getSkusToBeAdded(skuDatas: List<SkuData>): List<SkuData> {
        val matCodes = skuDatas.map { it.matCode }
        val nonExistingMatCodes = skuRepository.findNonExistingMatCodes(matCodes)
        val toBeAddedSkus = skuDatas.filter { nonExistingMatCodes.contains(it.matCode) }
        return toBeAddedSkus
    }

    private fun insertSku(skuDatas: List<SkuData>) {
        val now = LocalDateTime.now()
        Sku.batchInsert(skuDatas) {
            this[Sku.skuGroupId] = it.skuGroupId
            this[Sku.code] = it.code
            this[Sku.matCode] = it.matCode
            this[Sku.name] = it.name
            this[Sku.thickness] = it.thickness.setScale(5, RoundingMode.HALF_UP)
            this[Sku.thicknessUom] = it.thicknessUom
            this[Sku.width] = it.width.setScale(5, RoundingMode.HALF_UP)
            this[Sku.widthUom] = it.widthUom
            this[Sku.length] = it.length.setScale(5, RoundingMode.HALF_UP)
            this[Sku.lengthUom] = it.lengthUom
            this[Sku.circumference] = it.circumference.setScale(5, RoundingMode.HALF_UP)
            this[Sku.circumferenceUom] = it.circumferenceUom
            this[Sku.volumnM3] = it.volumnM3.setScale(5, RoundingMode.HALF_UP)
            this[Sku.volumnFt3] = it.volumnFt3.setScale(5, RoundingMode.HALF_UP)
            this[Sku.areaM2] = it.areaM2.setScale(5, RoundingMode.HALF_UP)
            this[Sku.species] = it.species
            this[Sku.fsc] = if(it.fsc == "Y") true else false
            this[Sku.grade] = it.grade ?: ""
            this[Sku.createdAt] = now
            this[Sku.updatedAt] = now
            this[Sku.status] = "A"
            if (it.extraAttributes.isNotEmpty()) {
                this[Sku.extraAttributes] = it.extraAttributes
            }
        }
    }
}