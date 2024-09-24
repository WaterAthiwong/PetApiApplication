package com.champaca.inventorydata.log

import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.log.model.StoredLog
import com.champaca.inventorydata.model.*
import org.jetbrains.exposed.sql.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import javax.sql.DataSource


@Service
class LogService(
    val dataSource: DataSource
) {

    @Value("\${champaca.reports.location}")
    lateinit var reportPath: String

    val productionDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun getExistingRefCodes(refCodes: List<String>): List<String> {
        return LotNo.slice(LotNo.refCode)
            .select { (LotNo.status eq "A") and (LotNo.refCode.inList(refCodes)) }
            .withDistinct()
            .map { resultRow -> resultRow[LotNo.refCode] }
    }

    fun getLogsByRefCodes(refCodes: List<String>): List<Pair<String, Int>> {
        val join = LotNoHasSku.join(LotNo, JoinType.INNER) { LotNoHasSku.lotNoId eq LotNo.id }
            .join(Sku, JoinType.INNER) {  LotNoHasSku.skuId eq Sku.id }
        val query = join.slice(Sku.matCode, LotNoHasSku.qty.sum())
            .select { (LotNo.refCode.inList(refCodes)) and (LotNo.status eq "A") and (Sku.status eq "A") }
            .groupBy(Sku.id)
        return query.map { resultRow -> Pair(resultRow[Sku.matCode], resultRow[LotNoHasSku.qty.sum()]!!.toInt()) }
    }

    fun getStoredLogs(searchParam: StoredLogSearchParam): List<StoredLog> {
        val join = Sku.join(SkuGroup, JoinType.INNER) { Sku.skuGroupId eq SkuGroup.id }
            .join(StoreLocationHasLotNo, JoinType.INNER) { StoreLocationHasLotNo.skuId eq Sku.id }
            .join(StoreLocation, JoinType.INNER) { StoreLocationHasLotNo.storeLocationId eq StoreLocation.id }
            .join(LotNo, JoinType.INNER) { StoreLocationHasLotNo.lotNoId eq LotNo.id}
            .join(GmItem, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(GoodMovement, JoinType.INNER) { GmItem.goodMovementId eq GoodMovement.id }
            .join(Supplier, JoinType.LEFT) { GoodMovement.supplierId eq Supplier.id }
        val query = join.select(Sku.matCode, Sku.length, Sku.lengthUom, Sku.circumference, Sku.circumferenceUom, Sku.grade, Sku.species,
            Sku.volumnM3, Sku.volumnFt3, LotNo.refCode, Supplier.name, GoodMovement.orderNo, GoodMovement.invoiceNo,
            GoodMovement.poNo, StoreLocation.code, StoreLocation.id, LotNo.id, LotNo.code, Sku.id, GoodMovement.id,
            LotNo.extraAttributes, GoodMovement.lotNo, GoodMovement.code, GoodMovement.productionDate)
            .where { (Sku.skuGroupId eq 1) and (Sku.status eq "A") and (LotNo.status eq "A") and
                    (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) and (GmItem.status eq "A")}

        if (searchParam.minLength != null) {
            query.andWhere { Sku.length greaterEq searchParam.minLength }
        }

        if (searchParam.minCircumference != null) {
            query.andWhere { Sku.circumference greaterEq searchParam.minCircumference }
        }

        if (!searchParam.suppliers.isNullOrEmpty()) {
            query.andWhere { Supplier.id.inList(searchParam.suppliers) }
        }

        if (!searchParam.refCodes.isNullOrEmpty()) {
            query.andWhere { LotNo.refCode.inList(searchParam.refCodes) }
        }

        return query.map { resultRow ->
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
}

data class StoredLogSearchParam(
    val minLength: Double? = null,
    val minCircumference: Double? = null,
    val suppliers: List<Int>? = listOf(),
    val refCodes: List<String>? = listOf()
)