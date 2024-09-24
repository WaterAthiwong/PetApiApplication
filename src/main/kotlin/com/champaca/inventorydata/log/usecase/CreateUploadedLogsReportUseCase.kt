package com.champaca.inventorydata.log.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.log.LogDeliverySearchParams
import com.champaca.inventorydata.log.LogDeliveryService
import com.champaca.inventorydata.log.LogSearchParams
import com.champaca.inventorydata.log.LogService
import com.champaca.inventorydata.log.model.LogData
import com.champaca.inventorydata.log.model.LogDeliveryData
import com.champaca.inventorydata.log.request.CreateUploadedLogsReportRequest
import com.champaca.inventorydata.masterdata.supplier.SupplierRepository
import com.champaca.inventorydata.utils.DateTimeUtil
import io.github.evanrupert.excelkt.Formula
import io.github.evanrupert.excelkt.workbook
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths
import java.text.DecimalFormat
import javax.sql.DataSource

@Service
class CreateUploadedLogsReportUseCase(
    val dataSource: DataSource,
    val logDeliveryService: LogDeliveryService,
    val supplierRepository: SupplierRepository,
    val dateTimeUtil: DateTimeUtil
) {

    @Value("\${champaca.reports.location}")
    lateinit var reportPath: String

    fun exeucte(request: CreateUploadedLogsReportRequest): File {
        var logDeliveries: List<LogDeliveryData> = listOf()
        var logs: List<LogData> = listOf()
        var fileName = ""
        Database.connect(dataSource)
        transaction {
            addLogger(ExposedInfoLogger)
            logs = logDeliveryService.getLogs(LogSearchParams(ids = request.logIds))
            val logDeliveryIds = logs.map { it.logDeliveryId }.toSet().toList()
            logDeliveries = logDeliveryService.getLogDelivery(LogDeliverySearchParams(ids = logDeliveryIds))

            fileName = createExcelFile("${reportPath}/logDelivery",
                "UploadedLogs_${dateTimeUtil.getCurrentDateTimeString("yyyy_MM_dd_HH_mm")}.xlsx", logDeliveries, logs)
        }
        return File(fileName)
    }

    private fun createExcelFile(dirName: String, fileName: String, logDeliveries: List<LogDeliveryData>, logs: List<LogData>): String {
        val suppliers = supplierRepository.getAll()
        Files.createDirectories(Paths.get(dirName))
        val fullFilePath = "${dirName}/${fileName}"
        var startRow = 1
        workbook {
            sheet {
                xssfSheet.setColumnWidth(0, 25 * 36)
                xssfSheet.setColumnWidth(1, 112 * 36)
                xssfSheet.setColumnWidth(2, 64 * 36)
                xssfSheet.setColumnWidth(3, 40 * 36)
                xssfSheet.setColumnWidth(4, 45 * 36)
                xssfSheet.setColumnWidth(5, 45 * 36)
                xssfSheet.setColumnWidth(6, 40 * 36)
                xssfSheet.setColumnWidth(7, 80 * 36)
                xssfSheet.setColumnWidth(8, 40 * 36)
                xssfSheet.setColumnWidth(9, 45 * 36)
                xssfSheet.setColumnWidth(10, 80 * 36)
                xssfSheet.setColumnWidth(11, 80 * 36)

                val headingStyle = createCellStyle {
                    setFont(createFont {
                        bold = true
                        fontName = "Cordia New"
                    })
                    alignment = HorizontalAlignment.CENTER
                    borderBottom = BorderStyle.THIN
                    borderTop = BorderStyle.THIN
                    borderLeft = BorderStyle.THIN
                    borderRight = BorderStyle.THIN
                }

                row(headingStyle) {
                    cell("No.")
                    cell("สวนป่า")
                    cell("สป.เล่มที่")
                    cell("ฉบับที่")
                    cell("บาร์โค้ด")
                    cell("เลขเขียง")
                    cell("เลขกอง")
                    cell("เลขที่ PO")
                    cell("ความโต")
                    cell("ความยาว")
                    cell("Mat Code")
                    cell("ปริมาตร (ลบ.ม.)")
                }

                val contentStyle = createCellStyle {
                    setFont(createFont {
                        fontName = "Cordia New"
                    })
                    borderBottom = BorderStyle.THIN
                    borderTop = BorderStyle.THIN
                    borderLeft = BorderStyle.THIN
                    borderRight = BorderStyle.THIN
                }
                val df = DecimalFormat("#.##")
                df.roundingMode = RoundingMode.CEILING
                logs.forEachIndexed { index, log ->
                    val logDelivery = logDeliveries.first { it.id == log.logDeliveryId }
                    val supplier = suppliers.first { it.id.value == logDelivery.supplierId }
                    row(contentStyle) {
                        cell(index + 1)
                        cell(supplier.name)
                        cell(logDelivery.forestryBook)
                        cell(logDelivery.forestryBookNo)
                        cell(log.refCode)
                        cell(log.logNo)
                        cell(log.batchNo)
                        cell(logDelivery.poNo)
                        cell(log.circumference)
                        cell(log.length)
                        cell(log.matCode)
                        cell(df.format(log.volumnM3).toDouble())
                    }
                }

                row {
                    cell(logs.size, contentStyle)
                    cell("")
                    cell("")
                    cell("")
                    cell("")
                    cell("")
                    cell("")
                    cell("")
                    cell("")
                    cell("")
                    cell("")
                    cell(Formula("SUM(L${startRow + 1}:L${startRow + logs.size})"), contentStyle)
                }
            }
        }.write(fullFilePath)
        return fullFilePath
    }
}