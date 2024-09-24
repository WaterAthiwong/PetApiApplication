package com.champaca.inventorydata.log.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.log.LogDeliveryService
import com.champaca.inventorydata.log.LogSearchParams
import com.champaca.inventorydata.log.LogService
import com.champaca.inventorydata.log.excel.LogFileRow
import com.champaca.inventorydata.log.excel.LogDetailParser
import com.champaca.inventorydata.log.request.ValidateForestryFileParams
import com.champaca.inventorydata.log.request.ValidateTransfromForestryFileParams
import com.champaca.inventorydata.log.response.*
import com.champaca.inventorydata.masterdata.sku.SkuRepository
import com.champaca.inventorydata.masterdata.supplier.SupplierRepository
import com.champaca.inventorydata.utils.DateTimeUtil
import io.github.evanrupert.excelkt.workbook
import org.apache.poi.ss.usermodel.BuiltinFormats
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.sql.DataSource

@Service
class ValidateForestryFileUseCase(
    val dataSource: DataSource,
    val dateTimeUtil: DateTimeUtil,
    val logDetailParser: LogDetailParser,
    val logService: LogService,
    val supplierRepository: SupplierRepository,
    val skuRepository: SkuRepository,
    val logDeliveryService: LogDeliveryService
) {

    companion object {
        val REFCODE_FORMAT = "[A-Z]\\d{6,7}".toRegex()
    }

    @Value("\${champaca.uploads.location}")
    lateinit var uploadRoot: String

    @Value("\${champaca.reports.location}")
    lateinit var reportPath: String

    val logger = LoggerFactory.getLogger(ValidateForestryFileUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun validate(file: MultipartFile, params: ValidateForestryFileParams): ValidateForestryFileResponse {
        val fileName = "logRefCodeCheck_${dateTimeUtil.getCurrentDateTimeString("yyyy_MM_dd_HH_mm_ss")}.xls"
        val rows = logDetailParser.parseFile(file, params.supplier, uploadRoot, fileName)
        return validate(rows, params)
    }

    fun validate(rows: List<LogFileRow>, params: ValidateForestryFileParams): ValidateForestryFileResponse {
        var invalidRefCodes: List<InvalidRefCode> = listOf()
        var duplicatedRefCodes: List<DuplicatedRefCode> = listOf()
        var nonExistingMatCode: List<NonExistingMatCode> = listOf()
        var refCodesExistingOnlyInFile: List<InvalidRefCode> = listOf()
        var refCodesExistingOnlyInBarcodeScanner: List<String> = listOf()

        val wrongFormatRefCodes = findWrongFormatRefCodes(rows)

        Database.connect(dataSource)
        transaction {
            addLogger(exposedLogger)

            invalidRefCodes = findExistingRefCodesInWms(rows) + findExistingRefCodesInLogDelivery(rows)
            duplicatedRefCodes = findDuplicatedRefCode(rows)
            nonExistingMatCode = findNonExistingMatCodes(rows, params.fsc)

            if (!params.scannedRefCodes.isNullOrEmpty()) {
                refCodesExistingOnlyInFile = rows.filter { !params.scannedRefCodes.contains(it.refCode) }.map {
                    val position = RowPosition(it.tabName, it.order, it.logNo)
                    InvalidRefCode(refCode = it.refCode, position = position)
                }
                val refCodes = rows.map { it.refCode }
                refCodesExistingOnlyInBarcodeScanner = params.scannedRefCodes.filter { !refCodes.contains(it) }
            }
        }

        val isValid = invalidRefCodes.isNullOrEmpty() && duplicatedRefCodes.isNullOrEmpty() && nonExistingMatCode.isNullOrEmpty()
                && refCodesExistingOnlyInFile.isNullOrEmpty() && refCodesExistingOnlyInBarcodeScanner.isNullOrEmpty()
        return ValidateForestryFileResponse(
            allRowsValid = isValid,
            wrongFormatRefCodes = wrongFormatRefCodes,
            existingRefCodes = invalidRefCodes,
            duplicatedRefCodes = duplicatedRefCodes,
            nonExistingMatCodes = nonExistingMatCode,
            refCodesExistOnlyInFile = refCodesExistingOnlyInFile,
            refCodesExistOnlyInBarcodeScanner = refCodesExistingOnlyInBarcodeScanner
        )
    }

    fun validateAndTransform(file: MultipartFile, params: ValidateTransfromForestryFileParams): ResultOf<File> {
        val fileName = "logRefCodeCheck_${dateTimeUtil.getCurrentDateTimeString("yyyy_MM_dd_HH_mm_ss")}.xls"
        val rows = logDetailParser.parseFile(file, params.supplier, uploadRoot, fileName)
        val validateResponse = validate(rows, params.toValidateForestryFileParams())
        return if(canTransformFile(validateResponse)) {
            val importFile = createFileForImportingLogs(rows, params)
            ResultOf.Success(importFile)
        } else {
            ResultOf.Failure("The file contains invalid data. Please check again!!")
        }
    }

    private fun canTransformFile(response: ValidateForestryFileResponse): Boolean {
        return when {
            !response.existingRefCodes.isNullOrEmpty() -> false
            !response.duplicatedRefCodes.isNullOrEmpty() -> false
            !response.refCodesExistOnlyInBarcodeScanner.isNullOrEmpty() -> false
            !response.refCodesExistOnlyInFile.isNullOrEmpty() -> false
            else -> true
        }
    }

    private fun createFileForImportingLogs(logDataRows: List<LogFileRow>, params: ValidateTransfromForestryFileParams): File {
        val dirName = "${reportPath}/importingLog"
        Files.createDirectories(Paths.get(dirName))
        val fileName = "${dirName}/ImportingLog_${dateTimeUtil.getCurrentDateTimeString("yyyy_MM_dd_HH_mm_ss")}.xlsx"

        var forestryName: String = ""
        transaction {
            forestryName = supplierRepository.getAll().find { it.id.value == params.supplier }?.name ?: ""
        }

        workbook {
            val textCellStyle = createCellStyle {
                setDataFormat(BuiltinFormats.getBuiltinFormat("text"))
            }
            sheet {
                row {
                    cell("MatCode")
                    cell("Ref Code")
                    cell("Qty")
                    cell("Store Location")
                    cell("Remark")
                    cell("Additional Field log_no")
                    cell("Additional Field volumn_m3")
                    cell("Additional Field supplier")
                    cell("Additional Field forestry_book")
                    cell("Additional Field forestry_book_no")
                }
                for(logDataRow in logDataRows) {
                    row {
                        cell(logDataRow.getMatCode(params.fsc))
                        cell(logDataRow.refCode)
                        cell(logDataRow.quantity)
                        cell("BSLYZ9999")
                        cell("")
                        cell(logDataRow.logNo)
                        cell(logDataRow.volumnM3)
                        cell(forestryName)
                        cell(params.forestryBook ?: "")
                        cell(params.forestryBookNo ?: "")
                    }
                }
            }
        }.write(fileName)
        return File(fileName)
    }

    private fun findWrongFormatRefCodes(rows:List<LogFileRow>): List<InvalidRefCode> {
        return rows.filter { !REFCODE_FORMAT.matches(it.refCode)||checkSumFormatRefCodes(it.refCode) }.map {
            val position = RowPosition(it.tabName, it.order, it.logNo)
            InvalidRefCode(
                refCode = it.refCode,
                position = position
            )
        }
    }

    private fun findExistingRefCodesInWms(rows: List<LogFileRow>): List<InvalidRefCode> {
        val refCodes = rows.map { it.refCode }.toSet().toList()
        val existingRefCodes = logService.getExistingRefCodes(refCodes)
        return rows.filter { existingRefCodes.contains(it.refCode) }.map {
            val position = RowPosition(it.tabName, it.order, it.logNo)
            InvalidRefCode(
                refCode = it.refCode,
                position = position
            )
        }
    }

    private fun findExistingRefCodesInLogDelivery(rows: List<LogFileRow>): List<InvalidRefCode> {
        val refCodes = rows.map { it.refCode }.toSet().toList()
        val existingRefCodes = logDeliveryService.getLogs(LogSearchParams(refCodes = refCodes)).map { it.refCode }
        return rows.filter { existingRefCodes.contains(it.refCode) }.map {
            val position = RowPosition(it.tabName, it.order, it.logNo)
            InvalidRefCode(
                refCode = it.refCode,
                position = position
            )
        }
    }

    private fun findDuplicatedRefCode(rows: List<LogFileRow>): List<DuplicatedRefCode> {
        val rowMap = rows.groupBy({it.refCode}, {it})
        val results: MutableList<DuplicatedRefCode> = mutableListOf()
        rowMap.forEach { key, value ->
            if(value.size > 1) {
                val positions = value.map { RowPosition(it.tabName, it.order, it.logNo) }
                results += DuplicatedRefCode(refCode = key, positions = positions)
            }
        }
        return results
    }

    private fun findNonExistingMatCodes(rows: List<LogFileRow>, fsc: Boolean): List<NonExistingMatCode> {
        val matCodes = rows.map { it.getMatCode(fsc) }.distinct()
        val nonExistingMatCodes = skuRepository.findNonExistingMatCodes(matCodes)
        return rows.filter { nonExistingMatCodes.contains(it.getMatCode(fsc)) }
            .map {
                val position = RowPosition(it.tabName, it.order, it.logNo)
                NonExistingMatCode(matCode = it.getMatCode(fsc), position = position)
            }
    }

    private fun checkSumFormatRefCodes(refCode : String): Boolean {

        if(refCode.length == 8){
            var sum = 0
            for (i in 1 until 7) {
                val digit = refCode[i].toString().toInt()
                sum += digit
            }
            val checksum = sum % 10
            return if (checksum.toString() == refCode[7].toString()) false else true
        }
        else{
             return false
        }
    }
}