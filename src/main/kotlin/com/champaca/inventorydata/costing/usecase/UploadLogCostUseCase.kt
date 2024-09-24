package com.champaca.inventorydata.costing.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ChampacaConstant.M3_TO_FT3
import com.champaca.inventorydata.costing.response.UploadLogCostResponse
import com.champaca.inventorydata.databasetable.RawMaterialCost
import com.champaca.inventorydata.databasetable.dao.RawMaterialCostDao
import com.champaca.inventorydata.utils.DateTimeUtil
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.FileInputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class UploadLogCostUseCase(
    val dataSource: DataSource,
    val dateTimeUtil: DateTimeUtil
) {
    @Value("\${champaca.uploads.location}")
    lateinit var uploadRoot: String

    val logger = LoggerFactory.getLogger(UploadLogCostUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(file: MultipartFile, supplierId: Int): UploadLogCostResponse {
        val fileName = "logCost_${dateTimeUtil.getCurrentDateTimeString("yyyy_MM_dd_HH_mm_ss")}.xls"
        val logCosts = parseFile(file, fileName)

        Database.connect(dataSource)
        var newPiles = listOf<Pair<String, BigDecimal>>()
        var existingPiles = listOf<Pair<String, BigDecimal>>()
        transaction {
            addLogger(exposedLogger)
            val existingPileNos = getExisitingPileNos(logCosts.map { it.first }, supplierId)
            newPiles = logCosts.filter { it.first !in existingPileNos }
            existingPiles = logCosts.filter { it.first in existingPileNos }
            insertNewPileNos(newPiles, supplierId)
            updateExistingPileNos(existingPiles, supplierId)
        }

        return UploadLogCostResponse.Success(newPiles.size, existingPiles.size)
    }

    private fun parseFile(file: MultipartFile, fileName: String): List<Pair<String, BigDecimal>> {
        val uploadRootPath = Paths.get(uploadRoot)
        Files.createDirectories(uploadRootPath)
        Files.copy(file.inputStream, uploadRootPath.resolve(fileName))
        val filePath = "${uploadRootPath}/${fileName}"

        val inputStream = FileInputStream(filePath)
        val xlWb = WorkbookFactory.create(inputStream)
        val results: MutableList<Pair<String, BigDecimal>> = mutableListOf()
        val sheet = xlWb.getSheetAt(0)
        // Reference for using physicalNumberOfRows https://www.baeldung.com/java-excel-find-last-row#2-using-getphysicalnumberofrows
        for(rowIndex in 1 until sheet.physicalNumberOfRows) {
            val row = sheet.getRow(rowIndex)
            // Check the condition that the cell may not be an actual order number cell.
            val pileNoCellValue = row?.getCell(0)
            if (pileNoCellValue == null) {
                continue
            }
            val pileNo = pileNoCellValue.stringCellValue
            if (pileNo.isNullOrEmpty()) {
                continue
            }

            val costCell = row.getCell(2)
            if (costCell == null) {
                continue
            }
            val cost = costCell.numericCellValue.toBigDecimal()
            results.add(pileNo to cost)
        }
        return results
    }

    private fun getExisitingPileNos(pileNos: List<String>, supplierId: Int): List<String> {
        val query = RawMaterialCost.select(RawMaterialCost.poNo)
            .where { (RawMaterialCost.status eq "A") and (RawMaterialCost.type eq RawMaterialCostDao.LOG) and
                (RawMaterialCost.poNo inList pileNos) and (RawMaterialCost.supplierId eq supplierId) }
        return query.map { it[RawMaterialCost.poNo] }
    }

    private fun insertNewPileNos(newPiles: List<Pair<String, BigDecimal>>, supplierId: Int) {
        val now = LocalDateTime.now()
        RawMaterialCost.batchInsert(newPiles) {
            val unitCostFt3 = it.second.divide(M3_TO_FT3, 4, RoundingMode.HALF_UP)
            this[RawMaterialCost.supplierId] = supplierId
            this[RawMaterialCost.type] = RawMaterialCostDao.LOG
            this[RawMaterialCost.poNo] = it.first
            this[RawMaterialCost.unitCostM3] = it.second
            this[RawMaterialCost.unitCostFt3] = unitCostFt3
            this[RawMaterialCost.createdAt] = now
            this[RawMaterialCost.updatedAt] = now
            this[RawMaterialCost.status] = "A"
        }
    }

    private fun updateExistingPileNos(existingPiles: List<Pair<String, BigDecimal>>, supplierId: Int) {
        val now = LocalDateTime.now()
        existingPiles.forEach { (pileNo, cost) ->
            RawMaterialCost.update({ (RawMaterialCost.poNo eq pileNo) and (RawMaterialCost.type eq RawMaterialCostDao.LOG) and (RawMaterialCost.supplierId eq supplierId) }) {
                it[RawMaterialCost.unitCostM3] = cost
                it[RawMaterialCost.unitCostFt3] = cost.divide(M3_TO_FT3, 4, RoundingMode.HALF_UP)
                it[RawMaterialCost.updatedAt] = now
            }
        }
    }
}