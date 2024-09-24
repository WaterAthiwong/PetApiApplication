package com.champaca.inventorydata.log.excel

import com.champaca.inventorydata.model.Species
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths

@Service
class LogDetailParser(
    val logDataPositions: Map<Int, LogFileDataPosition>
) {

    companion object {
        val COLUMNS = listOf<Char>('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
            'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z')
        val ORDER_NO_CELL_TYPES = listOf<CellType>(CellType.NUMERIC, CellType.FORMULA)
    }

    fun parseFile(file: MultipartFile, supplierId: Int, uploadPath: String, uploadFileName: String): List<LogFileRow> {
        if(!canParseFile(supplierId)) {
            throw java.lang.IllegalArgumentException("Can't read the file from supplier: ${supplierId}.")
        }

        val uploadRootPath = Paths.get(uploadPath)
        Files.createDirectories(uploadRootPath)
        Files.copy(file.inputStream, uploadRootPath.resolve(uploadFileName))

        return readFile("${uploadPath}/${uploadFileName}", supplierId)
    }

    private fun canParseFile(supplierId: Int): Boolean {
        return logDataPositions.containsKey(supplierId)
    }

    private fun readFile(filePath: String, supplierId: Int): List<LogFileRow> {
        // Reference: https://chercher.tech/kotlin/read-write-excel-kotlin
        val inputStream = FileInputStream(filePath)
        var xlWb = WorkbookFactory.create(inputStream)
        val results: MutableList<LogFileRow> = mutableListOf()
        val fileDetail = logDataPositions[supplierId]!!
        for(sheetIndex in 0 until xlWb.numberOfSheets) {
            val sheet = xlWb.getSheetAt(sheetIndex)
            val sheetName = sheet.sheetName
            // Reference for using physicalNumberOfRows https://www.baeldung.com/java-excel-find-last-row#2-using-getphysicalnumberofrows
            for(rowIndex in fileDetail.startRow until sheet.physicalNumberOfRows) {
                val row = sheet.getRow(rowIndex)
                // Check the condition that the cell may not be an actual order number cell.
                val orderCellValue = row?.getCell(columnIndex(fileDetail.orderColumn))
                if (orderCellValue == null || !ORDER_NO_CELL_TYPES.contains(orderCellValue.cellType)) {
                    continue
                }
                val order = orderCellValue.numericCellValue.toInt()
                if (order == 0) {
                    continue
                }

                val species = row.getCell(columnIndex(fileDetail.speciesColumn)).stringCellValue
                val length = row.getCell(columnIndex(fileDetail.lengthColumn)).numericCellValue.toInt()
                val circumference = row.getCell(columnIndex(fileDetail.circumferenceColumn)).numericCellValue.toInt()
                val logNoRow = row.getCell(columnIndex(fileDetail.logNoColumn))
                val logNo = if (logNoRow.cellType == CellType.NUMERIC) {
                    logNoRow.numericCellValue.toInt().toString()
                } else {
                    logNoRow.stringCellValue
                }
                val volumnM3 = row.getCell(columnIndex(fileDetail.volumnM3Column)).numericCellValue.toBigDecimal()
                val refCode = row.getCell(columnIndex(fileDetail.barcodeColumn)).stringCellValue

                results.add(
                    LogFileRow(
                        tabName = sheetName,
                        order = order,
                        species = Species.PT,
                        length = length,
                        circumference = circumference,
                        logNo = logNo,
                        quantity = 1,
                        volumnM3 = volumnM3,
                        refCode = refCode,
                    )
                )
            }
        }
        return results
    }

    private fun columnIndex(column: Char): Int {
        return COLUMNS.indexOf(column)
    }
}