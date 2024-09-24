package com.champaca.inventorydata.data.report.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.data.report.TransactionService
import com.champaca.inventorydata.data.report.request.GetPileTransactionRequest
import com.champaca.inventorydata.data.report.response.PileTransactionEntry
import com.champaca.inventorydata.masterdata.manufacturingline.ManufacturingLineRepository
import com.champaca.inventorydata.masterdata.skugroup.SkuGroupRepository
import com.champaca.inventorydata.utils.DateTimeUtil
import io.github.evanrupert.excelkt.workbook
import org.apache.commons.compress.utils.IOUtils
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class GetPileTransactionUseCase(
    val dataSource: DataSource,
    val transactionService: TransactionService,
    val dateTimeUtil: DateTimeUtil,
    val skuGroupRepository: SkuGroupRepository,
    val manufacturingLineRepository: ManufacturingLineRepository
) {
    @Value("\${champaca.reports.location}")
    lateinit var reportPath: String

    @Value("\${champaca.inventoryData.resource.location}")
    lateinit var reportResourcePath: String

    val logger = LoggerFactory.getLogger(GetPileTransactionUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: GetPileTransactionRequest, isPick: Boolean): List<PileTransactionEntry> {
        Database.connect(dataSource)

        var results = listOf<PileTransactionEntry>()
        transaction {
            addLogger(exposedLogger)
            results= transactionService.getPileTransaction(request, isPick)
        }
        return results
    }

    val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    fun executeExcelPerJobReport(request: GetPileTransactionRequest, isPick: Boolean): File {
        Database.connect(dataSource)

        var transactionEntries = listOf<PileTransactionEntry>()
        transaction {
            addLogger(exposedLogger)
            transactionEntries= transactionService.getPileTransaction(request, isPick)
        }

        val dirName = "${reportPath}/transaction"
        Files.createDirectories(Paths.get(dirName))
        val fileName = "${dirName}/ReportEachJob_${dateTimeUtil.getCurrentDateTimeString("yyyy_MM_dd_HH_mm")}.xlsx"
        val transactionByGoodMovement = transactionEntries.groupBy { it.goodMovementCode }
        var sheetIndex = 1
        val firstDataRow = 6
        workbook {
            transactionByGoodMovement.forEach { (gmCode, entries) ->
                val jobNo = entries.first().jobNo ?: ""
                var index = 1

                val headerStyle = createCellStyle {
                    setFont(createFont {
                        fontHeightInPoints = 16
                        bold = true
                    })
                    alignment = HorizontalAlignment.CENTER
                }
                val attrHeadingStyle = createCellStyle {
                    setFont(createFont {
                        fontHeightInPoints = 11
                        bold = true
                    })
                }
                val attrDataStyle = createCellStyle {
                    setFont(createFont {
                        fontHeightInPoints = 11
                    })
                    alignment = HorizontalAlignment.LEFT
                }
                val dataHeadingStyle = createCellStyle {
                    setFont(createFont {
                        fontHeightInPoints = 11
                        bold = true
                    })
                    borderBottom = BorderStyle.THIN
                    borderLeft = BorderStyle.THIN
                    borderRight = BorderStyle.THIN
                    borderTop = BorderStyle.THIN
                    alignment = HorizontalAlignment.CENTER
                }
                val wrapHeadingStyle = dataHeadingStyle.copy()!!.apply { setWrapText(true) }
                val dataStyle = createCellStyle {
                    setDataFormat(BuiltinFormats.getBuiltinFormat("text"))
                    setFont(createFont {
                        fontHeightInPoints = 11
                    })
                    borderBottom = BorderStyle.THIN
                    borderLeft = BorderStyle.THIN
                    borderRight = BorderStyle.THIN
                    borderTop = BorderStyle.THIN
                }
                val wrapDataStyle = dataStyle.copy()!!.apply { setWrapText(true) }

                sheet(gmCode) {
                    // Set paper size to A4
                    xssfSheet.printSetup.apply {
                        paperSize = PrintSetup.A4_PAPERSIZE
                        landscape = true
                    }

                    // Set margins to narrow
                    xssfSheet.apply {
                        setMargin(Sheet.LeftMargin, 0.25) // Narrow left margin
                        setMargin(Sheet.RightMargin, 0.25) // Narrow right margin
                        setMargin(Sheet.TopMargin, 0.75) // Narrow top margin
                        setMargin(Sheet.BottomMargin, 0.75) // Narrow bottom margin
                    }

                    xssfSheet.apply {
                        setColumnWidth(0, textColumnWidth(3))
                        setColumnWidth(1, textColumnWidth(21))
                        setColumnWidth(2, textColumnWidth(40))
                        setColumnWidth(3, textColumnWidth(12))
                        setColumnWidth(4, textColumnWidth(4))
                        setColumnWidth(5, textColumnWidth(4))
                        setColumnWidth(6, textColumnWidth(4))
                        setColumnWidth(7, textColumnWidth(8))
                        setColumnWidth(8, textColumnWidth(8))
                        setColumnWidth(9, textColumnWidth(12))
                        setColumnWidth(10, textColumnWidth(13))
                    }

                    val footer: Footer = xssfSheet.footer
                    footer.left = entries.first().jobNo
                    footer.right = "&P / &N"

                    row {
                        val header = headerText(isPick, request, entries.first())
                        cell(header, headerStyle)
                    }
                    mergeCell(xssfSheet, 0, 0, 0, 10)

                    row {
                        cell("")
                        cell("Job Order No", attrHeadingStyle)
                        cell(jobNo, attrDataStyle)
                        cell("เลขที่เอกสาร", attrHeadingStyle)
                        cell("")
                        cell(entries.first().goodMovementOrderNo ?: "", attrDataStyle)
                    }
                    row {
                        cell("")
                        val customer = customer(isPick, request, entries.first())
                        cell(customer.first, attrHeadingStyle)
                        cell(customer.second, attrDataStyle)
                        cell("วันที่", attrHeadingStyle)
                        cell("")
                        cell(dateFormat.format(entries.first().date), attrDataStyle)
                    }
                    row {}
                    row {
                        cell("No", dataHeadingStyle)
                        cell("Mat Code", dataHeadingStyle)
                        cell("ชนิดไม้", dataHeadingStyle)
                        cell("กอง", dataHeadingStyle)
                        cell("ขนาด", dataHeadingStyle)
                        cell("", dataHeadingStyle)
                        cell("", dataHeadingStyle)
                        cell("จำนวนแผ่น", wrapHeadingStyle)
                        cell("ปริมาตร ลบ.ฟ.", wrapHeadingStyle)
                        cell("Invoice No", wrapHeadingStyle)
                        cell("หมายเหตุ", wrapHeadingStyle)
                        cell("Order No", wrapHeadingStyle)
                    }

                    row {
                        cell("", dataHeadingStyle)
                        cell("", dataHeadingStyle)
                        cell("", dataHeadingStyle)
                        cell("", dataHeadingStyle)
                        cell("หนา", dataHeadingStyle)
                        cell("กว้าง", dataHeadingStyle)
                        cell("ยาว", dataHeadingStyle)
                        cell("", dataHeadingStyle)
                        cell("", dataHeadingStyle)
                        cell("", dataHeadingStyle)
                        cell("", dataHeadingStyle)
                        cell("", dataHeadingStyle)
                    }

                    mergeCell(xssfSheet, 4, 4, 4, 6)
                    mergeCell(xssfSheet, 4, 5, 0, 0)
                    mergeCell(xssfSheet, 4, 5, 1, 1)
                    mergeCell(xssfSheet, 4, 5, 2, 2)
                    mergeCell(xssfSheet, 4, 5, 3, 3)
                    mergeCell(xssfSheet, 4, 5, 7, 7)
                    mergeCell(xssfSheet, 4, 5, 8, 8)
                    mergeCell(xssfSheet, 4, 5, 9, 9)
                    mergeCell(xssfSheet, 4, 5, 10, 10)
                    mergeCell(xssfSheet, 4, 5, 11, 11)

                    for(entry in entries) {
                        row {
                            cell(index, dataStyle)
                            cell(entry.matCode, wrapDataStyle)
                            cell(entry.skuName, wrapDataStyle)
                            cell(entry.pileCode, dataStyle)
                            cell(entry.thickness.setScale(2, RoundingMode.HALF_UP), dataStyle)
                            cell(entry.width.setScale(2, RoundingMode.HALF_UP), dataStyle)
                            cell(entry.length.setScale(2, RoundingMode.HALF_UP), dataStyle)
                            cell("${entry.qty}", dataStyle)
                            cell(entry.totalVolumnFt3.setScale(2, RoundingMode.HALF_UP), dataStyle)
                            cell(entry.invoiceNo ?: "", wrapDataStyle)
                            cell(entry.remark ?: "", wrapDataStyle)
                            cell(entry.orderNo ?: "", wrapDataStyle)
                        }
                        index++
                    }

                    row {}
                    row {
                        cell("หมายเหตุ")
                    }
                    row {}
                    row {}

                    val footerFilePath = "${reportResourcePath}/images/PickReportFooter.png"
                    addImage(xssfWorkbook, xssfSheet, footerFilePath, xssfSheet.lastRowNum + 1, xssfSheet.lastRowNum + 7, 0, 9)
                }
                sheetIndex++
            }
        }.write(fileName)
        return File(fileName)
    }

    val locationNameMap = mapOf(29 to "คลังไม้แปรรูปสำเร็จ")

    private fun textColumnWidth(textLength: Int) = (textLength + 1) * 256
    private fun headerText(isPick: Boolean, request: GetPileTransactionRequest, firstEntry: PileTransactionEntry): String {
        val manufacturingLine = firstEntry.manufacturingLine ?: ""
        return if (isPick) {
            if (manufacturingLine.isNotBlank()) {
                "ใบเบิกไม้เข้า $manufacturingLine"
            } else {
                "ใบเบิกไม้จาก${firstEntry.department}"
            }
        } else {
            if (manufacturingLine.isNotBlank()) {
                "ใบรับไม้จาก $manufacturingLine"
            } else {
                "ใบรับไม้เข้า${firstEntry.department}"
            }
        }
    }

    private fun customer(isPick: Boolean, request: GetPileTransactionRequest, firstEntry: PileTransactionEntry): Pair<String, String> {
        val supplier = firstEntry.supplier ?: ""
        val manufacturingLine = firstEntry.manufacturingLine ?: ""
        return if (isPick) {
            if (supplier.isNotBlank()) {
                Pair("ผู้ขอเบิก", supplier)
            } else {
                Pair("เบิกเข้า", manufacturingLine)
            }
        }
        else {
            if (supplier.isNotBlank()) {
                Pair("ผู้ส่งมอบ", supplier)
            } else {
                Pair("รับจาก", manufacturingLine)
            }
        }
    }

    private fun mergeCell(sheet: Sheet, firstRow: Int, lastRow: Int, firstCol: Int, lastCol: Int) {
        val merge = CellRangeAddress(firstRow, lastRow, firstCol, lastCol)
        sheet.addMergedRegion(merge)
    }

    private fun addImage(workbook: Workbook, sheet: Sheet, filePath: String, firstRow: Int, lastRow: Int, firstCol: Int, lastCol: Int) {
        val inputStream = FileInputStream(filePath)
        val bytes: ByteArray = IOUtils.toByteArray(inputStream)
        val pictureIndex: Int = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG)
        inputStream.close()

        // Create drawing patriarch to manage images
        val drawing: Drawing<*> = sheet.createDrawingPatriarch()

        // Create an anchor and specify upper-left corner of the picture
        val anchor: ClientAnchor = drawing.createAnchor(2, 0, 40, 0, firstCol, firstRow, lastCol, lastRow)
        anchor.anchorType = ClientAnchor.AnchorType.MOVE_DONT_RESIZE

        // Create the picture
        drawing.createPicture(anchor, pictureIndex)
    }

    fun executeExcelAllJobsReport(request: GetPileTransactionRequest, isPick: Boolean): File {
        Database.connect(dataSource)

        var transactionEntries = listOf<PileTransactionEntry>()
        transaction {
            addLogger(ExposedInfoLogger)
            transactionEntries= transactionService.getPileTransaction(request, isPick)
        }

        val dirName = "${reportPath}/transaction"
        Files.createDirectories(Paths.get(dirName))
        val fileName = "${dirName}/ReportAllJobs_${dateTimeUtil.getCurrentDateTimeString("yyyy_MM_dd_HH_mm")}.xlsx"
        workbook {
            val dataHeadingStyle = createCellStyle {
                setFont(createFont {
                    fontHeightInPoints = 10
                    bold = true
                })
                borderBottom = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
                borderTop = BorderStyle.THIN
                alignment = HorizontalAlignment.CENTER
            }
            val wrapHeadingStyle = dataHeadingStyle.copy()!!.apply { setWrapText(true) }
            val dataStyle = createCellStyle {
                setDataFormat(BuiltinFormats.getBuiltinFormat("text"))
                setFont(createFont {
                    fontHeightInPoints = 10
                })
                borderBottom = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
                borderTop = BorderStyle.THIN
            }
            val wrapDataStyle = dataStyle.copy()!!.apply { setWrapText(true) }

            sheet {
                // Set paper size to A4
                xssfSheet.printSetup.apply {
                    paperSize = PrintSetup.A4_PAPERSIZE
                    landscape = true
                }

                // Set margins to narrow
                xssfSheet.apply {
                    setMargin(Sheet.LeftMargin, 0.25) // Narrow left margin
                    setMargin(Sheet.RightMargin, 0.25) // Narrow right margin
                    setMargin(Sheet.TopMargin, 0.75) // Narrow top margin
                    setMargin(Sheet.BottomMargin, 0.75) // Narrow bottom margin
                }

                xssfSheet.apply {
                    setColumnWidth(0, textColumnWidth(3))
                    setColumnWidth(1, textColumnWidth(14))
                    setColumnWidth(2, textColumnWidth(20))
                    setColumnWidth(3, textColumnWidth(10))
                    setColumnWidth(4, textColumnWidth(16))
                    setColumnWidth(5, textColumnWidth(30))
                    setColumnWidth(6, textColumnWidth(5))
                    setColumnWidth(7, textColumnWidth(11))
                    setColumnWidth(8, textColumnWidth(3))
                    setColumnWidth(9, textColumnWidth(3))
                    setColumnWidth(10, textColumnWidth(3))
                    setColumnWidth(11, textColumnWidth(3))
                    setColumnWidth(12, textColumnWidth(4))
                    setColumnWidth(13, textColumnWidth(4))
                    setColumnWidth(14, textColumnWidth(4))
                    setColumnWidth(15, textColumnWidth(9))
                    setColumnWidth(16, textColumnWidth(9))
                    setColumnWidth(17, textColumnWidth(14))
                    setColumnWidth(18, textColumnWidth(14))
                    setColumnWidth(19, textColumnWidth(9))
                }

                xssfSheet.footer.apply {
                    right = "&P / &N"
                    left = if (request.fromProductionDate == request.toProductionDate) {
                        "วันที่ ${request.fromProductionDate}"
                    } else {
                        "วันที่ ${request.fromProductionDate} - ${request.toProductionDate}"
                    }
                }

                row {
                    cell("No", dataHeadingStyle)
                    cell("Job No", dataHeadingStyle)
                    cell("Supplier", dataHeadingStyle)
                    cell("เครื่องเลื่อย", dataHeadingStyle)
                    cell("Mat Code", dataHeadingStyle)
                    cell("ชื่อ", dataHeadingStyle)
                    cell("ชนิดไม้", dataHeadingStyle)
                    cell("กอง", dataHeadingStyle)
                    cell("ขนาด", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("เกรด", dataHeadingStyle)
                    cell("แผ่น", wrapHeadingStyle)
                    cell("ลบ.ฟ", wrapHeadingStyle)
                    cell("ตร.ม.", wrapHeadingStyle)
                    cell("Order No", wrapHeadingStyle)
                    cell("หมายเหตุ", wrapHeadingStyle)
                    cell("วันที่ผลิต (ตามใบเบิก/รับ)", wrapHeadingStyle)
                    cell("ลูกค้า", wrapHeadingStyle)
                    cell("วันทีทำรายการ", wrapHeadingStyle)
                }

                row {
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("หนา", dataHeadingStyle)
                    cell("กว้าง", dataHeadingStyle)
                    cell("ยาว", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                    cell("", dataHeadingStyle)
                }

                mergeCell(xssfSheet, 0, 0, 8, 10)
                mergeCell(xssfSheet, 0, 1, 0, 0)
                mergeCell(xssfSheet, 0, 1, 1, 1)
                mergeCell(xssfSheet, 0, 1, 2, 2)
                mergeCell(xssfSheet, 0, 1, 3, 3)
                mergeCell(xssfSheet, 0, 1, 4, 4)
                mergeCell(xssfSheet, 0, 1, 5, 5)
                mergeCell(xssfSheet, 0, 1, 6, 6)
                mergeCell(xssfSheet, 0, 1, 7, 7)
                mergeCell(xssfSheet, 0, 1, 11, 11)
                mergeCell(xssfSheet, 0, 1, 12, 12)
                mergeCell(xssfSheet, 0, 1, 13, 13)
                mergeCell(xssfSheet, 0, 1, 14, 14)
                mergeCell(xssfSheet, 0, 1, 15, 15)
                mergeCell(xssfSheet, 0, 1, 16, 16)
                mergeCell(xssfSheet, 0, 1, 17, 17)
                mergeCell(xssfSheet, 0, 1, 18, 18)
                mergeCell(xssfSheet, 0, 1, 19, 19)

                var index = 1
                for(entry in transactionEntries) {
                    row {
                        cell(index, dataStyle)
                        cell(entry.jobNo ?: "", dataStyle)
                        cell(entry.supplier?.replace("สวนป่า", "") ?: "", dataStyle)
                        cell(entry.manufacturingLine ?: "", dataStyle)
                        cell(entry.matCode, wrapDataStyle)
                        cell(entry.skuName, wrapDataStyle)
                        cell(entry.species ?: "", wrapDataStyle)
                        cell(entry.pileCode, dataStyle)
                        cell(entry.thickness.setScale(2, RoundingMode.HALF_UP), dataStyle)
                        cell(entry.width.setScale(2, RoundingMode.HALF_UP), dataStyle)
                        cell(entry.length.setScale(2, RoundingMode.HALF_UP), dataStyle)
                        cell(entry.grade ?: "", dataStyle)
                        cell("${entry.qty}", dataStyle)
                        cell(entry.totalVolumnFt3.setScale(2, RoundingMode.HALF_UP), dataStyle)
                        cell(entry.totalAreaM2.setScale(2, RoundingMode.HALF_UP), dataStyle)
                        cell(entry.orderNo ?: "", wrapDataStyle)
                        cell(entry.remark ?: "", wrapDataStyle)
                        cell(entry.date.format(dateFormat), dataStyle)
                        cell(entry.customer, wrapDataStyle)
                        cell(entry.transactionAt.format(dateFormat), dataStyle)
                    }
                    index++
                }
            }
        }.write(fileName)
        return File(fileName)
    }
}