package com.champaca.inventorydata.data.stock

import com.champaca.inventorydata.data.stock.request.StockInProcessRequest
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.model.ItemStock
import com.champaca.inventorydata.data.stock.request.StockInStorageRequest
import org.apache.commons.text.StringEscapeUtils
import org.jetbrains.exposed.sql.*
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import javax.sql.DataSource

@Service
class StockService(
    val dataSource: DataSource
) {

    fun getStockInStorage(request: StockInStorageRequest): List<ItemStock> {
        val originalGoodMovement = GoodMovement.alias("originalGoodMovement")
        val join = Sku.join(StoreLocationHasLotNo, JoinType.INNER) { StoreLocationHasLotNo.skuId eq Sku.id }
            .join(StoreLocation, JoinType.INNER) { StoreLocationHasLotNo.storeLocationId eq StoreLocation.id }
            .join(LotNo, JoinType.INNER) { StoreLocationHasLotNo.lotNoId eq LotNo.id }
            .join(PileHasLotNo, JoinType.LEFT) { PileHasLotNo.lotNoId eq LotNo.id }
            .join(Pile, JoinType.LEFT) { (Pile.id eq PileHasLotNo.pileId) and (Pile.status eq "A") and (Pile.lotSet eq PileHasLotNo.lotSet) }
            .join(GmItem, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(GoodMovement, JoinType.INNER) { GmItem.goodMovementId eq GoodMovement.id }
            .join(originalGoodMovement, JoinType.INNER) { originalGoodMovement[GoodMovement.id] eq Pile.originGoodMovementId }
            .join(Supplier, JoinType.LEFT) { originalGoodMovement[GoodMovement.supplierId] eq Supplier.id }
            .join(SkuGroup, JoinType.INNER) { SkuGroup.id eq Sku.skuGroupId }
        val query = join.select(
            GoodMovement.jobNo,
            Pile.id,
            Pile.code,
            Sku.matCode,
            StoreLocationHasLotNo.qty,
            Sku.volumnFt3,
            StoreLocation.code,
            Supplier.name,
            originalGoodMovement[GoodMovement.poNo],
            originalGoodMovement[GoodMovement.invoiceNo],
            GoodMovement.productionDate,
            Pile.remark,
            Pile.orderNo,
            Pile.extraAttributes,
            Sku.thickness,
            Sku.width,
            Sku.length,
            Sku.grade,
            Sku.areaM2,
            Sku.thicknessUom,
            Sku.widthUom,
            Sku.lengthUom,
            SkuGroup.erpMainGroupName,
            SkuGroup.erpGroupCode,
            SkuGroup.erpGroupName,
            Sku.species
        )
            .where { (Sku.status eq "A") and (LotNo.status eq "A") and (GmItem.status eq "A") and
                    (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) }

        if (!request.species.isNullOrEmpty()) {
            query.andWhere { Sku.species eq request.species }
        }
        if (request.thickness != null) {
            query.andWhere { Sku.thickness eq request.thickness }
        }
        if (request.width != null) {
            query.andWhere { Sku.width eq request.width }
        }
        if (request.length != null) {
            query.andWhere { Sku.length eq request.length }
        }
        if (!request.grade.isNullOrEmpty()) {
            query.andWhere { Sku.grade eq request.grade }
        }
        if (!request.locationPattern.isNullOrEmpty()) {
            query.andWhere { StoreLocation.code like stringLiteral("${request.locationPattern}%") }
        }
        if (!request.locations.isNullOrEmpty()) {
            query.andWhere { StoreLocation.code inList request.locations }
        }

        return query.map {  resultRow ->
            var supplier = ""
            resultRow[Supplier.name]?.let {
                supplier = StringEscapeUtils.unescapeHtml4(it)
            }
            ItemStock(
                jobNo = resultRow[GoodMovement.jobNo],
                pileId = resultRow[Pile.id].value,
                pileCode = resultRow[Pile.code],
                matCode = resultRow[Sku.matCode],
                qty = resultRow[StoreLocationHasLotNo.qty],
                volumnFt3 = resultRow[Sku.volumnFt3],
                location = resultRow[StoreLocation.code],
                supplier = supplier,
                orderNo = resultRow[Pile.orderNo],
                poNo = resultRow[originalGoodMovement[GoodMovement.poNo]],
                invoiceNo = resultRow[originalGoodMovement[GoodMovement.invoiceNo]],
                productionDate = resultRow[GoodMovement.productionDate].toString(),
                remark = resultRow[Pile.remark],
                extra = resultRow[Pile.extraAttributes] ?: mapOf(),
                thickness = resultRow[Sku.thickness].setScale(2, RoundingMode.HALF_UP),
                width = resultRow[Sku.width].setScale(2, RoundingMode.HALF_UP),
                length = resultRow[Sku.length].setScale(2, RoundingMode.HALF_UP),
                grade = resultRow[Sku.grade],
                areaM2 = resultRow[Sku.areaM2] ?: BigDecimal.ZERO,
                thicknessUom = resultRow[Sku.thicknessUom],
                widthUom = resultRow[Sku.widthUom],
                lengthUom = resultRow[Sku.lengthUom],
                mainGroupName = resultRow[SkuGroup.erpMainGroupName],
                groupName = "${resultRow[SkuGroup.erpGroupCode]} - ${resultRow[SkuGroup.erpGroupName]}",
                species = resultRow[Sku.species]
            )
        }
    }

    fun getStockInProcess(request: StockInProcessRequest): List<ItemStock> {
        val originalGoodMovement = GoodMovement.alias("originalGoodMovement")
        val join = Sku.join(ManufacturingLineHasLotNo, JoinType.INNER) { ManufacturingLineHasLotNo.skuId eq Sku.id }
            .join(ManufacturingLine, JoinType.INNER) { ManufacturingLineHasLotNo.manufacturingLineId eq ManufacturingLine.id }
            .join(LotNo, JoinType.INNER) { ManufacturingLineHasLotNo.lotNoId eq LotNo.id }
            .join(PileHasLotNo, JoinType.LEFT) { PileHasLotNo.lotNoId eq LotNo.id }
            .join(Pile, JoinType.LEFT) { (Pile.id eq PileHasLotNo.pileId) and (Pile.status eq "A") and (Pile.lotSet eq PileHasLotNo.lotSet) }
            .join(GmItem, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(GoodMovement, JoinType.INNER) { GmItem.goodMovementId eq GoodMovement.id }
            .join(originalGoodMovement, JoinType.INNER) { originalGoodMovement[GoodMovement.id] eq Pile.originGoodMovementId }
            .join(Supplier, JoinType.LEFT) { originalGoodMovement[GoodMovement.supplierId] eq Supplier.id }
            .join(SkuGroup, JoinType.INNER) { SkuGroup.id eq Sku.skuGroupId }
        val query = join.select(
            GoodMovement.jobNo,
            Pile.id,
            Pile.code,
            Sku.matCode,
            ManufacturingLineHasLotNo.qty,
            Sku.volumnFt3,
            Sku.areaM2,
            ManufacturingLine.name,
            Supplier.name,
            originalGoodMovement[GoodMovement.poNo],
            originalGoodMovement[GoodMovement.invoiceNo],
            GoodMovement.productionDate,
            Pile.remark,
            Pile.orderNo,
            Pile.extraAttributes,
            Sku.thickness,
            Sku.width,
            Sku.length,
            Sku.grade,
            Sku.areaM2,
            Sku.thicknessUom,
            Sku.widthUom,
            Sku.lengthUom,
            SkuGroup.erpMainGroupName,
            SkuGroup.erpGroupCode,
            SkuGroup.erpGroupName,
            Sku.species
        )
            .where { (Sku.status eq "A") and (LotNo.status eq "A") and (GmItem.status eq "A") and
                    (ManufacturingLine.processTypeId eq request.processId) and
                    (GoodMovement.type eq GoodMovementType.PICKING_ORDER.wmsName)  }

        if (!request.manufacturingLineIds.isNullOrEmpty()) {
            query.andWhere { ManufacturingLine.id inList request.manufacturingLineIds }
        }

        return query.map {  resultRow ->
            var supplier = ""
            resultRow[Supplier.name]?.let {
                supplier = StringEscapeUtils.unescapeHtml4(it)
            }
            ItemStock(
                jobNo = resultRow[GoodMovement.jobNo],
                pileId = resultRow[Pile.id].value,
                pileCode = resultRow[Pile.code],
                matCode = resultRow[Sku.matCode],
                qty = resultRow[ManufacturingLineHasLotNo.qty],
                volumnFt3 = resultRow[Sku.volumnFt3],
                manufacturingLine = resultRow[ManufacturingLine.name],
                supplier = supplier,
                orderNo = resultRow[Pile.orderNo],
                poNo = resultRow[originalGoodMovement[GoodMovement.poNo]],
                invoiceNo = resultRow[originalGoodMovement[GoodMovement.invoiceNo]],
                productionDate = resultRow[GoodMovement.productionDate].toString(),
                remark = resultRow[Pile.remark],
                extra = resultRow[Pile.extraAttributes] ?: mapOf(),
                thickness = resultRow[Sku.thickness].setScale(2, RoundingMode.HALF_UP),
                width = resultRow[Sku.width].setScale(2, RoundingMode.HALF_UP),
                length = resultRow[Sku.length].setScale(2, RoundingMode.HALF_UP),
                grade = resultRow[Sku.grade],
                areaM2 = resultRow[Sku.areaM2] ?: BigDecimal.ZERO,
                thicknessUom = resultRow[Sku.thicknessUom],
                widthUom = resultRow[Sku.widthUom],
                lengthUom = resultRow[Sku.lengthUom],
                mainGroupName = resultRow[SkuGroup.erpMainGroupName],
                groupName = "${resultRow[SkuGroup.erpGroupCode]} - ${resultRow[SkuGroup.erpGroupName]}",
                species = resultRow[Sku.species]
            )
        }
    }
}