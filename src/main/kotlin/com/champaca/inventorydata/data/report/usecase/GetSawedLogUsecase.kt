package com.champaca.inventorydata.data.report.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.data.report.request.GetSawedLogRequest
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.model.ProcessedLog
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class GetSawedLogUsecase(
    val dataSource: DataSource
) {
    companion object {
        val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    val logger = LoggerFactory.getLogger(GetSawedLogUsecase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: GetSawedLogRequest): List<ProcessedLog> {
        Database.connect(dataSource)

        var results: List<ProcessedLog> = listOf()
        transaction {
            addLogger(exposedLogger)
            results = getSawedLogs(request)
        }

        return results
    }

    private fun getSawedLogs(request: GetSawedLogRequest): List<ProcessedLog> {
        val join = GoodMovement.join(GmItem, JoinType.INNER) { GmItem.goodMovementId eq GoodMovement.id }
            .join(LotNo, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(Sku, JoinType.INNER) { GmItem.skuId eq Sku.id }
            .join(ManufacturingLine, JoinType.LEFT) { GoodMovement.manufacturingLineId eq ManufacturingLine.id }
        val query = join.select(
            GoodMovement.code, GoodMovement.productionDate, Sku.skuGroupId, Sku.matCode, Sku.length, Sku.circumference,
            Sku.volumnM3, Sku.volumnFt3, Sku.species, Sku.fsc, LotNo.refCode, LotNo.extraAttributes, GmItem.qty,
            ManufacturingLine.name, GoodMovement.jobNo, GoodMovement.orderNo, GoodMovement.poNo, GoodMovement.lotNo,
            GoodMovement.invoiceNo)
            .where{ (Sku.skuGroupId eq 1) and (LotNo.status eq "A") and (Sku.status eq "A") and (GmItem.status eq "A") and
                    (GoodMovement.type eq GoodMovementType.PICKING_ORDER.wmsName) }


        if (!request.startDate.isNullOrEmpty()) {
            query.andWhere { GoodMovement.productionDate greaterEq LocalDate.parse(request.startDate, DATE_FORMAT) }
        }

        if (!request.endDate.isNullOrEmpty()) {
            query.andWhere { GoodMovement.productionDate lessEq LocalDate.parse(request.endDate, DATE_FORMAT) }
        }

        val results = query.map { resultRow ->
            ProcessedLog(
                goodsMovementCode = resultRow[GoodMovement.code],
                productionDate = resultRow[GoodMovement.productionDate],
                skuGroupId = resultRow[Sku.skuGroupId],
                matCode = resultRow[Sku.matCode],
                length = resultRow[Sku.length],
                circumference = resultRow[Sku.circumference],
                skuVolumnM3 = resultRow[Sku.volumnM3],
                skuVolumnFt3 = resultRow[Sku.volumnFt3],
                species = resultRow[Sku.species],
                fsc = resultRow[Sku.fsc],
                refCode = resultRow[LotNo.refCode],
                extraAttributes = resultRow[LotNo.extraAttributes],
                qty = resultRow[GmItem.qty],
                manufacturingLine = resultRow[ManufacturingLine.name],
                jobNo = resultRow[GoodMovement.jobNo],
                orderNo = resultRow[GoodMovement.orderNo],
                poNo = resultRow[GoodMovement.poNo],
                invoiceNo = resultRow[GoodMovement.invoiceNo],
                lotNo = resultRow[GoodMovement.lotNo]
            )
        }

        return results
    }
}