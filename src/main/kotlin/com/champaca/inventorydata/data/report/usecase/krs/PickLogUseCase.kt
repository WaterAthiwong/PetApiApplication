package com.champaca.inventorydata.data.report.usecase.krs

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.common.ChampacaConstant.M3_TO_FT3
import com.champaca.inventorydata.data.report.TransactionService
import com.champaca.inventorydata.data.report.request.GetPileTransactionRequest
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.model.Species
import com.champaca.inventorydata.utils.DateTimeUtil
import io.github.evanrupert.excelkt.workbook
import org.apache.poi.ss.usermodel.BuiltinFormats
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class PickLogUseCase(
    val dataSource: DataSource,
    val dateTimeUtil: DateTimeUtil,
    val transactionService: TransactionService
) {
    companion object {
        const val SAWMILL_DEPARTMENT_ID = 1
        val PRODUCTION_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    @Value("\${champaca.reports.location}")
    lateinit var reportPath: String
    fun execute(pickDateFrom: String, pickDateTo: String): File {
        Database.connect(dataSource)

        var transactions = listOf<ResultRow>()
        transaction {
            addLogger(ExposedInfoLogger)

            val request = GetPileTransactionRequest(
                departmentId = SAWMILL_DEPARTMENT_ID,
                fromProductionDate = pickDateFrom,
                toProductionDate = pickDateTo,
                code = null,
                fromTransactionDate = null,
                toTransactionDate = null
            )
            transactions = transactionService.getLogTransaction(request)
        }

        val dirName = "${reportPath}/transaction/krs"
        Files.createDirectories(Paths.get(dirName))
        val fileName = "${dirName}/PickLog_${dateTimeUtil.getCurrentDateTimeString("yyyy_MM_dd_HH_mm")}.xlsx"
        workbook {
            val textCellStyle = createCellStyle {
                setDataFormat(BuiltinFormats.getBuiltinFormat("text"))
            }
            sheet {
                row {
                    cell("Grpmain")
                    cell("GrpMainName")
                    cell("ProdGrp")
                    cell("ProdGrpName")
                    cell("Woodtype")
                    cell("WoodTypeName")
                    cell("CerCode")
                    cell("CERTIFICATION")
                    cell("MainUnits")
                    cell("CutNumber")
                    cell("Book_Num")
                    cell("Book_No")
                    cell("Thick")
                    cell("Wide")
                    cell("Length")
                    cell("LengthUnit")
                    cell("Qty")
                    cell("M3")
                    cell("F3")
                    cell("Job No")
                    cell("Machine")
                    cell("Supplier")
                    cell("ProductionDate")
                    cell("Po No")
                    cell("Lot No")
                    cell("Invoice No")
                }

                for (entry in transactions) {
                    row {
                        val species = entry[Sku.species]
                        val fsc = entry[Sku.fsc]
                        val lotExtraAttributes = entry[LotNo.extraAttributes]
                        val volumnM3 = lotExtraAttributes?.get("volumnM3")?.toBigDecimal() ?: 0.toBigDecimal()
                        cell(1)
                        cell("RM")
                        cell("M0")
                        cell("LOG")
                        cell(species)
                        cell(Species.valueOf(species).longName ?: "")
                        cell(if (fsc) 1 else 2)
                        cell(if (fsc) "FSC" else "NON FSC")
                        cell("ท่อน")
                        cell(lotExtraAttributes?.get("logNo") ?: "")
                        cell(lotExtraAttributes?.get("forestryBook") ?: "")
                        cell(lotExtraAttributes?.get("forestryBookNo") ?: "")
                        cell(0.toBigDecimal().setScale(2, RoundingMode.HALF_UP))
                        cell(entry[Sku.circumference].setScale(2, RoundingMode.HALF_UP))
                        cell(entry[Sku.length].setScale(2, RoundingMode.HALF_UP))
                        cell("เมตร")
                        cell(1)
                        cell(volumnM3.setScale(3, RoundingMode.HALF_UP))
                        cell(volumnM3.multiply(M3_TO_FT3).setScale(3, RoundingMode.HALF_UP))
                        cell(entry[GoodMovement.jobNo] ?: "")
                        cell(entry[ManufacturingLine.name])
                        cell(entry[Supplier.name] ?: "")
                        cell(PRODUCTION_DATE_FORMAT.format(entry[GoodMovement.productionDate]))
                        cell(entry[GoodMovement.poNo] ?: "")
                        cell(entry[GoodMovement.lotNo] ?: "")
                        cell(entry[GoodMovement.invoiceNo] ?: "")
                    }
                }
            }
        }.write(fileName)
        return File(fileName)
    }
}