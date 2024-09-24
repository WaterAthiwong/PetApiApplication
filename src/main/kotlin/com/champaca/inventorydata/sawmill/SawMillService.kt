package com.champaca.inventorydata.sawmill

import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.LotNoDao
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.log.model.StoredLog
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class SawMillService() {
    val productionDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun getStoredLog(lotNo: LotNoDao): StoredLog {
        val join = Sku.join(SkuGroup, JoinType.INNER) { Sku.skuGroupId eq SkuGroup.id }
            .join(StoreLocationHasLotNo, JoinType.INNER) { StoreLocationHasLotNo.skuId eq Sku.id }
            .join(StoreLocation, JoinType.INNER) { StoreLocationHasLotNo.storeLocationId eq StoreLocation.id }
            .join(LotNo, JoinType.INNER) { StoreLocationHasLotNo.lotNoId eq LotNo.id}
            .join(GmItem, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(GoodMovement, JoinType.INNER) { GmItem.goodMovementId eq GoodMovement.id }
            .join(Supplier, JoinType.LEFT) { GoodMovement.supplierId eq Supplier.id }
        val query = join.select(
            Sku.matCode, Sku.length, Sku.lengthUom, Sku.circumference, Sku.circumferenceUom, Sku.grade, Sku.species,
            Sku.volumnM3, Sku.volumnFt3, LotNo.refCode, Supplier.name, GoodMovement.orderNo, GoodMovement.invoiceNo,
            GoodMovement.poNo, StoreLocation.code, StoreLocation.id, LotNo.id, LotNo.code, Sku.id, GoodMovement.id,
            LotNo.extraAttributes, GoodMovement.lotNo, GoodMovement.code, GoodMovement.productionDate)
            .where { (LotNo.status eq "A") and (GmItem.status eq "A") and (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) and
                    (LotNo.id eq lotNo.id) }
        return query.single().let { resultRow ->
            StoredLog(
                matCode = resultRow[Sku.matCode],
                length = resultRow[Sku.length],
                lengthUom = resultRow[Sku.lengthUom],
                circumference = resultRow[Sku.circumference],
                circumferenceUom = resultRow[Sku.circumferenceUom],
                grade = resultRow[Sku.grade],
                species = resultRow[Sku.species],
                skuVolumnM3 = resultRow[Sku.volumnM3],
                skuVolumnFt3 = resultRow[Sku.volumnFt3],
                refCode = resultRow[LotNo.refCode],
                supplierName = resultRow[Supplier.name],
                orderNo = resultRow[GoodMovement.orderNo],
                invoiceNo = resultRow[GoodMovement.invoiceNo],
                poNo = resultRow[GoodMovement.poNo],
                location = resultRow[StoreLocation.code],
                storeLocationId = resultRow[StoreLocation.id].value,
                skuId = resultRow[Sku.id].value,
                lotNo = resultRow[GoodMovement.lotNo] ?: "",
                lotNoId = resultRow[LotNo.id].value,
                goodMovementId = resultRow[GoodMovement.id].value,
                goodMovementCode = resultRow[GoodMovement.code],
                productionDate = productionDateFormat.format(resultRow[GoodMovement.productionDate]),
                lotExtraAttributes = resultRow[LotNo.extraAttributes]
            )
        }
    }

    fun hasLogBeenPicked(lotNo: LotNoDao): Boolean {
        val joins = GmItem.join(LotNo, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(GoodMovement, JoinType.INNER) { GmItem.goodMovementId eq GoodMovement.id }
        val query = joins.select(GmItem.id, GoodMovement.type)
            .where { (LotNo.id eq lotNo.id) and (GmItem.status eq "A") }
        val rows = query.toList()
        val goodMovementTypes = rows.map { it[GoodMovement.type] }
        return rows.size == 2 && goodMovementTypes.contains(GoodMovementType.PICKING_ORDER.wmsName) &&
                goodMovementTypes.contains(GoodMovementType.GOODS_RECEIPT.wmsName)
    }
}