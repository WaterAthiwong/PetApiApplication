package com.champaca.inventorydata.common

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.model.ProcessedWood
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class ProcessedWoodCommonService(
    val dataSource: DataSource
) {
    fun getManufacturedItemsFromProcess(processPrefix: String,
                           manuLines: List<String> = listOf(),
                           goodMovementCodes: List<String> = listOf(),
                           startDate: String = "",
                           endDate: String = ""): List<ProcessedWood> {
        var results: List<ProcessedWood> = listOf()

        Database.connect(dataSource)
        transaction {
            addLogger(ExposedInfoLogger)

            val join = GoodMovement.join(GmItem, JoinType.INNER) { GmItem.goodMovementId eq GoodMovement.id }
                .join(LotNo, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
                .join(LotGroup, JoinType.LEFT) { LotNo.lotGroupId eq LotGroup.id }
                .join(Sku, JoinType.INNER) { GmItem.skuId eq Sku.id }
                .join(ManufacturingLine, JoinType.INNER) { GoodMovement.manufacturingLineId eq ManufacturingLine.id }
                .join(ProcessType, JoinType.INNER) { ManufacturingLine.processTypeId eq ProcessType.id }
                .join(StoreLocationHasLotNo, JoinType.LEFT) { StoreLocationHasLotNo.lotNoId eq LotNo.id }
                .join(StoreLocation, JoinType.INNER) { StoreLocationHasLotNo.storeLocationId eq StoreLocation.id }
            val query = join.slice(GoodMovement.code, GoodMovement.productionDate, ProcessType.name, ManufacturingLine.name, LotGroup.code, Sku.matCode, Sku.width, Sku.widthUom, Sku.length, Sku.lengthUom, Sku.thickness, Sku.thicknessUom, Sku.volumnFt3, Sku.volumnM3, Sku.grade, Sku.fsc, Sku.species, GoodMovement.jobNo, GoodMovement.orderNo, GoodMovement.poNo, GoodMovement.invoiceNo, GmItem.qty, LotNo.updatedAt, LotNo.additionalField, StoreLocation.code)
                .select { (LotNo.status eq "A") and (Sku.status eq "A") and (ProcessType.prefix eq processPrefix) and
                        (GoodMovement.type eq "good receipt") and (GmItem.status eq "A") }

            if (manuLines.isNotEmpty()) {
                query.andWhere { ManufacturingLine.name.inList(manuLines) }
            }

            if (goodMovementCodes.isNotEmpty()) {
                query.andWhere { GoodMovement.code.inList(goodMovementCodes) }
            }

            if (startDate.isNotBlank()) {
                query.andWhere { GoodMovement.productionDate greaterEq stringLiteral(startDate) }
            }

            if (endDate.isNotBlank()) {
                query.andWhere { GoodMovement.productionDate lessEq stringLiteral(endDate) }
            }

            results = query.map { resultRow ->
                ProcessedWood(
                    goodsMovementCode = resultRow[GoodMovement.code],
                    productionDate = resultRow[GoodMovement.productionDate],
                    process = resultRow[ProcessType.name],
                    manufacturingLine = resultRow[ManufacturingLine.name],
                    palletCode = resultRow[LotGroup.code],
                    matCode = resultRow[Sku.matCode],
                    width = resultRow[Sku.width],
                    widthUom = resultRow[Sku.widthUom],
                    length = resultRow[Sku.length],
                    lengthUom = resultRow[Sku.lengthUom],
                    thickness = resultRow[Sku.thickness],
                    thicknessUom = resultRow[Sku.thicknessUom],
                    volumnFt3 = resultRow[Sku.volumnFt3],
                    volumnM3 = resultRow[Sku.volumnM3],
                    grade = resultRow[Sku.grade],
                    fsc = resultRow[Sku.fsc],
                    species = resultRow[Sku.species],
                    jobNo = resultRow[GoodMovement.jobNo],
                    orderNo = resultRow[GoodMovement.orderNo],
                    poNo = resultRow[GoodMovement.poNo],
                    invoiceNo = resultRow[GoodMovement.invoiceNo],
                    quantity = resultRow[GmItem.qty].toInt(),
                    updatedAt = resultRow[LotNo.updatedAt],
                    additionalDataStr = resultRow[LotNo.additionalField]
                ).apply {
                    location = resultRow[StoreLocation.code]
                }
            }
        }

        return results
    }

    fun getStockInProcess(prefix: String, manufacturingLineNames: List<String>): List<ProcessedWood> {
        var results: List<ProcessedWood> = listOf()

        Database.connect(dataSource)

        transaction {
            addLogger(ExposedInfoLogger)

            val join = ManufacturingLineHasLotNo.join(ManufacturingLine, JoinType.INNER) { ManufacturingLineHasLotNo.manufacturingLineId eq ManufacturingLine.id }
                .join(ProcessType, JoinType.INNER) { ManufacturingLine.processTypeId eq ProcessType.id }
                .join(Sku, JoinType.INNER) { ManufacturingLineHasLotNo.skuId eq Sku.id }
                .join(LotNo, JoinType.INNER) { ManufacturingLineHasLotNo.lotNoId eq LotNo.id }
                .join(LotGroup, JoinType.LEFT) { LotNo.lotGroupId eq LotGroup.id }
                .join(GmItem, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
                .join(GoodMovement, JoinType.INNER) { (GmItem.goodMovementId eq GoodMovement.id) and (GoodMovement.manufacturingLineId eq ManufacturingLineHasLotNo.manufacturingLineId) }

            val query = join.slice(GoodMovement.code, GoodMovement.productionDate, ProcessType.name, ManufacturingLine.name,
                LotGroup.code, Sku.matCode, Sku.width, Sku.widthUom, Sku.length, Sku.lengthUom, Sku.thickness, Sku.thicknessUom,
                Sku.volumnFt3, Sku.volumnM3, Sku.grade, Sku.fsc, Sku.species, GoodMovement.jobNo, GoodMovement.orderNo,
                GoodMovement.poNo, GoodMovement.invoiceNo, ManufacturingLineHasLotNo.qty, LotNo.updatedAt, LotNo.additionalField)
                .select { (LotNo.status eq "A") and (Sku.status eq "A") and (ProcessType.prefix eq prefix) and (GmItem.status eq "A") }

            if (manufacturingLineNames.isNotEmpty()) {
                query.andWhere { ManufacturingLine.name.inList(manufacturingLineNames) }
            }

            results = query.map { resultRow ->
                ProcessedWood(
                    goodsMovementCode = resultRow[GoodMovement.code],
                    productionDate = resultRow[GoodMovement.productionDate],
                    process = resultRow[ProcessType.name],
                    manufacturingLine = resultRow[ManufacturingLine.name],
                    palletCode = resultRow[LotGroup.code],
                    matCode = resultRow[Sku.matCode],
                    width = resultRow[Sku.width],
                    widthUom = resultRow[Sku.widthUom],
                    length = resultRow[Sku.length],
                    lengthUom = resultRow[Sku.lengthUom],
                    thickness = resultRow[Sku.thickness],
                    thicknessUom = resultRow[Sku.thicknessUom],
                    volumnFt3 = resultRow[Sku.volumnFt3],
                    volumnM3 = resultRow[Sku.volumnM3],
                    grade = resultRow[Sku.grade],
                    fsc = resultRow[Sku.fsc],
                    species = resultRow[Sku.species],
                    jobNo = resultRow[GoodMovement.jobNo],
                    orderNo = resultRow[GoodMovement.orderNo],
                    poNo = resultRow[GoodMovement.poNo],
                    invoiceNo = resultRow[GoodMovement.invoiceNo],
                    quantity = resultRow[ManufacturingLineHasLotNo.qty].toInt(),
                    updatedAt = resultRow[LotNo.updatedAt],
                    additionalDataStr = resultRow[LotNo.additionalField]
                )
            }
        }

        return results
    }
}