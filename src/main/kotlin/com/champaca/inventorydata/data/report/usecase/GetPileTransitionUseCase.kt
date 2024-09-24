package com.champaca.inventorydata.data.report.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.SkuGroupDao
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.skugroup.SkuGroupRepository
import com.champaca.inventorydata.model.Species
import com.champaca.inventorydata.data.report.request.GetPileTransitionRequest
import com.champaca.inventorydata.data.report.response.PileTransitionEntry
import com.champaca.inventorydata.utils.DateTimeUtil
import io.github.evanrupert.excelkt.workbook
import org.apache.poi.ss.usermodel.BuiltinFormats
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class GetPileTransitionUseCase(
    val dataSource: DataSource,
    val dateTimeUtil: DateTimeUtil,
    val skuGroupRepository: SkuGroupRepository,
) {
    @Value("\${champaca.reports.location}")
    lateinit var reportPath: String

    companion object {
        val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }

    val logger = LoggerFactory.getLogger(GetPileTransitionUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: GetPileTransitionRequest): List<PileTransitionEntry> {
        Database.connect(dataSource)

        var results = listOf<PileTransitionEntry>()
        transaction {
            addLogger(exposedLogger)
            results = query(request)
        }
        return results
    }

    fun executeToKrsFile(request: GetPileTransitionRequest): File {
        Database.connect(dataSource)

        var transitionEntries = listOf<PileTransitionEntry>()
        var skuGroups: Map<Int, SkuGroupDao> = mapOf()
        transaction {
            addLogger(exposedLogger)
            transitionEntries = query(request)
            skuGroups = skuGroupRepository.getAll().associateBy({it.id.value}, {it})
        }

        val dirName = "${reportPath}/erp/ti"
        Files.createDirectories(Paths.get(dirName))
        val fileName = "${dirName}/Ti_${dateTimeUtil.getCurrentDateTimeString("yyyy_MM_dd_HH_mm")}.xlsx"
        workbook {
            val textCellStyle = createCellStyle {
                setDataFormat(BuiltinFormats.getBuiltinFormat("text"))
            }
            sheet {
                xssfSheet.apply {
                    setColumnWidth(3, textColumnWidth(20))
                    setColumnWidth(5, textColumnWidth(11))
                }

                row {
                    cell("Grpmain")
                    cell("GrpMainName")
                    cell("ProdGrp")
                    cell("ProdGrpName")
                    cell("RefVoucherNo")
                    cell("Pile")
                    cell("Woodtype")
                    cell("WoodTypeName")
                    cell("CerCode")
                    cell("CERTIFICATION")
                    cell("MainUnits")
                    cell("Grade")
                    cell("Thick")
                    cell("Wide")
                    cell("Length")
                    cell("LengthUnit")
                    cell("Qty")
                    cell("F3")
                    cell("InvoiceNo")
                    cell("LotNo")
                    cell("Supplier")
                    cell("OrderNo")
                    cell("Destination")
                    cell("SawMill")
                    cell("KilnDry")
                    cell("Recut")
                    cell("RM")
                    cell("วันที่ย้าย")
                }

                for(entry in transitionEntries) {
                    val skuGroup = skuGroups.get(entry.skuGroupId)!!
                    row {
                        cell(skuGroup.erpMainGroupId)
                        cell(skuGroup.erpMainGroupName)
                        cell(skuGroup.erpGroupCode)
                        cell(skuGroup.erpGroupName)
                        cell("")
                        cell(entry.pileCode)
                        cell(entry.species)
                        cell(Species.valueOf(entry.species.trim())?.longName ?: "")
                        cell(if (entry.fsc) 1 else 2)
                        cell(if (entry.fsc) "FSC" else "NON FSC")
                        cell("PCS")
                        cell(entry.grade)
                        cell(entry.thickness.setScale(2, RoundingMode.HALF_UP))
                        cell(entry.width.setScale(2, RoundingMode.HALF_UP))
                        cell(entry.length.setScale(2, RoundingMode.HALF_UP))
                        cell("ฟุต")
                        cell(entry.qty.toBigDecimal().setScale(2, RoundingMode.HALF_UP))
                        cell(entry.totalVolumnFt3.setScale(2, RoundingMode.HALF_UP))
                        cell(entry.invoiceNo ?: "")
                        cell(entry.lotNo ?: "")
                        cell(entry.supplierName ?: "")
                        cell(entry.orderNo ?: "")
                        cell(entry.destination ?: "")
                        cell(entry.sm ?: "")
                        cell(entry.kd ?: "")
                        cell(entry.rc ?: "")
                        cell(entry.rm ?: "")
                        cell(DATE_FORMAT.format(entry.transitionDate))
                    }
                }
            }
        }.write(fileName)
        return File(fileName)
    }

    private fun textColumnWidth(textLength: Int) = (textLength + 1) * 256

    private fun query(request: GetPileTransitionRequest): List<PileTransitionEntry> {
        val fromLocation = StoreLocation.alias("fromLocation")
        val fromStoreZone = StoreZone.alias("fromStoreZone")
        val toLocation = StoreLocation.alias("toLocation")
        val toStoreZone = StoreZone.alias("toStoreZone")
        val originalGoodMovement = GoodMovement.alias("originalGoodMovement")
        val joins = PileRelocation.join(Pile, JoinType.INNER) { PileRelocation.pileId eq Pile.id }
            .join(fromLocation, JoinType.INNER) { PileRelocation.fromStoreLocationId eq fromLocation[StoreLocation.id] }
            .join(fromStoreZone, JoinType.INNER) { fromLocation[StoreLocation.storeZoneId] eq fromStoreZone[StoreZone.id] }
            .join(toLocation, JoinType.INNER) { PileRelocation.toStoreLocationId eq toLocation[StoreLocation.id] }
            .join(toStoreZone, JoinType.INNER) { toLocation[StoreLocation.storeZoneId] eq toStoreZone[StoreZone.id] }
            .join(PileHasLotNo, JoinType.INNER) { (PileHasLotNo.pileId eq Pile.id) and (PileHasLotNo.lotSet eq PileRelocation.lotSet) }
            .join(GmItem, JoinType.INNER) { GmItem.lotNoId eq PileHasLotNo.lotNoId }
            .join(LotNo, JoinType.INNER) { LotNo.id eq GmItem.lotNoId }
            .join(GoodMovement, JoinType.INNER) { GoodMovement.id eq GmItem.goodMovementId }
            .join(Sku, JoinType.INNER) { Sku.id eq GmItem.skuId }
            .join(User, JoinType.INNER) { User.id eq PileRelocation.userId }
            .join(originalGoodMovement, JoinType.INNER) { originalGoodMovement[GoodMovement.id] eq Pile.originGoodMovementId }
            .join(Supplier, JoinType.LEFT) { originalGoodMovement[GoodMovement.supplierId] eq Supplier.id }

        val query = joins.select(
            Pile.code,
            Pile.id,
            Sku.matCode,
            GmItem.skuId,
            Sku.thickness,
            Sku.width,
            Sku.length,
            Sku.lengthUom,
            Sku.fsc,
            Sku.grade,
            Sku.species,
            Sku.skuGroupId,
            GmItem.lotNoId,
            Pile.orderNo,
            GoodMovement.jobNo,
            GmItem.qty,
            Sku.volumnFt3,
            Pile.remark,
            PileRelocation.createdAt,
            User.firstname,
            User.lastname,
            fromLocation[StoreLocation.code],
            toLocation[StoreLocation.code],
            GoodMovement.code,
            Pile.extraAttributes,
            originalGoodMovement[GoodMovement.invoiceNo],
            originalGoodMovement[GoodMovement.lotNo],
            Supplier.name,
            toLocation[StoreLocation.code]
        ).where {
            (LotNo.status eq "A") and (Pile.status eq "A") and
                    (fromLocation[StoreLocation.status] eq "A") and
                    (toLocation[StoreLocation.status] eq "A") and
                    (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) and
                    (GmItem.status eq "A") and
                    (fromStoreZone[StoreZone.departmentId] eq request.fromDepartmentId) and
                    (toStoreZone[StoreZone.departmentId] eq request.toDepartmentId)
        }

        if (!request.fromTransitionDate.isNullOrEmpty()) {
            query.andWhere {
                PileRelocation.createdAt.date() greaterEq stringLiteral(request.fromTransitionDate)
            }
        }

        if (!request.toTransitionDate.isNullOrEmpty()) {
            query.andWhere {
                PileRelocation.createdAt.date() lessEq stringLiteral(request.toTransitionDate)
            }
        }

        return query.map { resultRow ->
            PileTransitionEntry(
                pileCode = resultRow[Pile.code],
                pileId = resultRow[Pile.id].value,
                matCode = resultRow[Sku.matCode],
                skuId = resultRow[GmItem.skuId],
                thickness = resultRow[Sku.thickness],
                width = resultRow[Sku.width],
                length = resultRow[Sku.length],
                lengthUom = resultRow[Sku.lengthUom],
                fsc = resultRow[Sku.fsc],
                grade = resultRow[Sku.grade] ?: "",
                species = resultRow[Sku.species],
                skuGroupId = resultRow[Sku.skuGroupId],
                lotId = resultRow[GmItem.lotNoId],
                orderNo = resultRow[Pile.orderNo],
                jobNo = resultRow[GoodMovement.jobNo],
                qty = resultRow[GmItem.qty].toInt(),
                volumnFt3 = resultRow[Sku.volumnFt3],
                remark = resultRow[Pile.remark],
                transitionDate = resultRow[PileRelocation.createdAt].toLocalDate(),
                user = "${resultRow[User.firstname]} ${resultRow[User.lastname]}",
                fromLocation = resultRow[fromLocation[StoreLocation.code]],
                toLocation = resultRow[toLocation[StoreLocation.code]],
                goodMovementCode = resultRow[GoodMovement.code],
                extraAttributes = resultRow[Pile.extraAttributes],
                invoiceNo = resultRow[originalGoodMovement[GoodMovement.invoiceNo]],
                lotNo = resultRow[originalGoodMovement[GoodMovement.lotNo]],
                supplierName = resultRow[Supplier.name],
                destination = resultRow[toLocation[StoreLocation.code]]
            )
        }
    }
}