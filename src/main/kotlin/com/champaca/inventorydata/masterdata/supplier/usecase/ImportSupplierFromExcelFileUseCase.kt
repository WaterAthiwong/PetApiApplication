package com.champaca.inventorydata.masterdata.supplier.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.Supplier
import com.champaca.inventorydata.databasetable.dao.SupplierDao
import com.champaca.inventorydata.masterdata.sku.response.ImportSkusFromExcelFileResponse
import com.champaca.inventorydata.masterdata.supplier.excel.SupplierExcelReader
import com.champaca.inventorydata.masterdata.supplier.model.SupplierData
import com.champaca.inventorydata.masterdata.supplier.response.ImportSuppliersFromExcelFileResponse
import com.champaca.inventorydata.utils.DateTimeUtil
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import javax.sql.DataSource

@Service
class ImportSupplierFromExcelFileUseCase(
    val datasource: DataSource,
    val excelReader: SupplierExcelReader,
    val dateTimeUtil: DateTimeUtil,
    val cacheManager: CacheManager
) {

    @Value("\${champaca.uploads.location}")
    lateinit var uploadRoot: String

    val logger = LoggerFactory.getLogger(ImportSupplierFromExcelFileUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(file: MultipartFile): ImportSuppliersFromExcelFileResponse {
        val uploadRootPath = Paths.get(uploadRoot)
        Files.createDirectories(uploadRootPath)
        val fileName = "ExcelSupplier_${dateTimeUtil.getCurrentDateTimeString("yyyy_MM_dd_HH_mm_ss")}.xlsx"
        Files.copy(file.inputStream, uploadRootPath.resolve(fileName))

        Database.connect(datasource)
        val supplierDatas = excelReader.readFile("${uploadRoot}/${fileName}")
        var toBeAddedSupplier = listOf<SupplierData>()
        transaction {
            addLogger(exposedLogger)
            toBeAddedSupplier = getSupplierToBeAdded(supplierDatas)
            if (toBeAddedSupplier.isNotEmpty()) {
                insertSupplier(toBeAddedSupplier)
            }
        }
        // ลบ cache ที่มี key เป็น matCode ของ sku ที่ถูกเพิ่มเข้ามา
        cacheManager.getCache("suppliers")?.clear()
        cacheManager.getCache("suppliersByType")?.clear()

        return ImportSuppliersFromExcelFileResponse.Success(
            existings = supplierDatas - toBeAddedSupplier,
            createds = toBeAddedSupplier
        )
    }

    private fun getSupplierToBeAdded(supplierDatas: List<SupplierData>): List<SupplierData> {
        val existingSuppliers = SupplierDao.find { (Supplier.name inList supplierDatas.map { it.name }) and (Supplier.status eq "A")}
        val existingSupplierNames = existingSuppliers.map { it.name }
        return supplierDatas.filter { !existingSupplierNames.contains(it.name) }
    }

    private fun insertSupplier(supplierDatas: List<SupplierData>) {
        Supplier.batchInsert(supplierDatas) {
            this[Supplier.type] = it.type
            this[Supplier.name] = it.name
            this[Supplier.erpCode] = it.erpCode
            this[Supplier.taxNo] = it.taxNo
            this[Supplier.address] = it.address
            this[Supplier.contact] = it.contact
            this[Supplier.phone] = it.phone
            this[Supplier.email] = it.email
            this[Supplier.remark] = it.remark
            this[Supplier.status] = "A"
        }
    }
}