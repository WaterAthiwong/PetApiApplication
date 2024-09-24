package com.champaca.inventorydata.data.report.usecase.krs

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.common.ChampacaConstant.M3_TO_FT3
import com.champaca.inventorydata.data.report.TransactionService
import com.champaca.inventorydata.data.report.request.GetPileTransactionRequest
import com.champaca.inventorydata.data.report.response.PileTransactionEntry
import com.champaca.inventorydata.databasetable.GoodMovement
import com.champaca.inventorydata.databasetable.LotNo
import com.champaca.inventorydata.databasetable.Sku
import com.champaca.inventorydata.databasetable.Supplier
import com.champaca.inventorydata.databasetable.dao.ManufacturingLineDao
import com.champaca.inventorydata.databasetable.dao.SkuGroupDao
import com.champaca.inventorydata.masterdata.manufacturingline.ManufacturingLineRepository
import com.champaca.inventorydata.masterdata.skugroup.SkuGroupRepository
import com.champaca.inventorydata.model.Species
import com.champaca.inventorydata.utils.DateTimeUtil
import io.github.evanrupert.excelkt.workbook
import org.apache.poi.ss.usermodel.BuiltinFormats
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.addLogger
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
class PickForSawUseCase(
    val dataSource: DataSource,
    val transactionService: TransactionService,
    val dateTimeUtil: DateTimeUtil,
    val skuGroupRepository: SkuGroupRepository,
    val manufacturingLineRepository: ManufacturingLineRepository
) {

    companion object {
        const val SAWMILL_DEPARTMENT_ID = 1
        val PRODUCTION_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    @Value("\${champaca.reports.location}")
    lateinit var reportPath: String
    fun execute(pickDateFrom: String, pickDateTo: String): File {
        Database.connect(dataSource)

        var pileTransactions = listOf<PileTransactionEntry>()
        var logTransactions = listOf<ResultRow>()
        var skuGroupMap: Map<Int, SkuGroupDao> = mapOf()
        var manuMap: Map<Int, ManufacturingLineDao> = mapOf()
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
            pileTransactions = transactionService.getPileTransaction(request, true)
                .filter { it.manufacturingLineId != null }
            logTransactions = transactionService.getLogTransaction(request)
            skuGroupMap = skuGroupRepository.getAll().associateBy({ it.id.value }, { it })
            manuMap = manufacturingLineRepository.getAll().associateBy({ it.id.value }, { it })
        }

        val dirName = "${reportPath}/transaction/krs"
        Files.createDirectories(Paths.get(dirName))
        val fileName = "${dirName}/PickForSaw_${dateTimeUtil.getCurrentDateTimeString("yyyy_MM_dd_HH_mm")}.xlsx"
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
                    cell("Grade")
                    cell("CerCode")
                    cell("CERTIFICATION")
                    cell("MainUnits")
                    cell("Machine")
                    cell("CutNumber")
                    cell("Book_Num")
                    cell("Book_No")
                    cell("Thick")
                    cell("Wide")
                    cell("Length")
                    cell("LengthUnit")
                    cell("Qty")
                    cell("F3")
                    cell("Job No")
                    cell("Machine")
                    cell("Supplier")
                    cell("ProductionDate")
                    cell("Po No")
                    cell("Lot No")
                    cell("Invoice No")
                }

                for (entry in pileTransactions) {
                    val skuGroup = skuGroupMap.get(entry.skuGroupId)!!
                    val manu = manuMap.get(entry.manufacturingLineId)!!
                    row {
                        cell(skuGroup.erpMainGroupId)
                        cell(skuGroup.erpMainGroupName)
                        cell(skuGroup.erpGroupCode)
                        cell(skuGroup.erpGroupName)
                        cell(entry.species)
                        cell(Species.valueOf(entry.species)?.longName ?: "")
                        cell(entry.grade)
                        cell(if (entry.fsc) 1 else 2)
                        cell(if (entry.fsc) "FSC" else "NON FSC")
                        cell("PCS")
                        cell(manu.erpMachineCode ?: "")
                        cell(entry.pileCode)
                        cell("")
                        cell("")
                        cell(entry.thickness.setScale(2, RoundingMode.HALF_UP))
                        cell(entry.width.setScale(2, RoundingMode.HALF_UP))
                        cell(entry.length.setScale(2, RoundingMode.HALF_UP))
                        cell("ฟุต")
                        cell(entry.qty.setScale(2, RoundingMode.HALF_UP))
                        cell(entry.totalVolumnFt3.setScale(2, RoundingMode.HALF_UP))
                        cell(entry.jobNo ?: "")
                        cell(entry.manufacturingLine ?: "")
                        cell(entry.supplier ?: "")
                        cell(PRODUCTION_DATE_FORMAT.format(entry.productionDate))
                        cell(entry.poNo ?: "")
                        cell(entry.lotNo ?: "")
                        cell(entry.invoiceNo ?: "")
                    }
                }

                for (entry in logTransactions) {
                    row {
                        val species = entry[Sku.species]
                        val fsc = entry[Sku.fsc]
                        val lotExtraAttributes = entry[LotNo.extraAttributes]
                        val volumnM3 = lotExtraAttributes?.get("volumnM3")?.toBigDecimal() ?: 0.toBigDecimal()
                        val manu = manuMap.get(entry[GoodMovement.manufacturingLineId])!!
                        cell(1)
                        cell("RM")
                        cell("M0")
                        cell("LOG")
                        cell(species)
                        cell(Species.valueOf(species).longName ?: "")
                        cell("")
                        cell(if (fsc) 1 else 2)
                        cell(if (fsc) "FSC" else "NON FSC")
                        cell("ท่อน")
                        cell(manu.erpMachineCode ?: "")
                        cell(lotExtraAttributes?.get("logNo") ?: "")
                        cell(lotExtraAttributes?.get("forestryBook") ?: "")
                        cell(lotExtraAttributes?.get("forestryBookNo") ?: "")
                        cell(0.toBigDecimal().setScale(2, RoundingMode.HALF_UP))
                        cell(entry[Sku.circumference].setScale(2, RoundingMode.HALF_UP))
                        cell(entry[Sku.length].setScale(2, RoundingMode.HALF_UP))
                        cell("เมตร")
                        cell(1.toBigDecimal().setScale(2, RoundingMode.HALF_UP))
                        cell(volumnM3.multiply(M3_TO_FT3).setScale(2, RoundingMode.HALF_UP))
                        cell(entry[GoodMovement.jobNo] ?: "")
                        cell(manu.name ?: "")
                        cell(entry[Supplier.name] ?: "")
                        cell(entry[GoodMovement.productionDate])
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