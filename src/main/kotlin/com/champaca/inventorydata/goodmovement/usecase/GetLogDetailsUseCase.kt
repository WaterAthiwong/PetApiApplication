package com.champaca.inventorydata.goodmovement.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.GoodMovementDao
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.goodmovement.response.GetLogDetailsResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class GetLogDetailsUseCase(
    val dataSource: DataSource
) {

    val RECORED_DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    val logger = LoggerFactory.getLogger(GetLogDetailsUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(goodMovementId: Int): GetLogDetailsResponse {
        Database.connect(dataSource)

        var result: GetLogDetailsResponse? = null
        transaction {
            addLogger(exposedLogger)
            val goodMovement = GoodMovementDao.findById(goodMovementId)
            var logs = getLogs(goodMovementId)
            if (goodMovement!!.type == GoodMovementType.GOODS_RECEIPT.wmsName) {
                result = GetLogDetailsResponse(
                    logs = logs,
                    itemCount = logs.size,
                    totalVolumnM3 = logs.sumOf { it.volumnM3 }.setScale(2, RoundingMode.HALF_UP)
                )
            } else {
                val unpickedLogs = getUnPickedLogs(logs.map { it.lotNoId }, goodMovementId)
                result = GetLogDetailsResponse(
                    logs = logs,
                    itemCount = logs.size,
                    totalVolumnM3 = logs.sumOf { it.volumnM3 }.setScale(2, RoundingMode.HALF_UP),
                    unPickedLogs = unpickedLogs,
                    unPickedLogItemCount = unpickedLogs.size,
                    unPIckedLogTotalVolumnM3 = unpickedLogs.sumOf { it.volumnM3 }.setScale(2, RoundingMode.HALF_UP)
                )
            }
        }
        return result!!
    }

    private fun getLogs(goodMovementId: Int): List<GetLogDetailsResponse.Log> {
        val joins = GmItem.join(GoodMovement, JoinType.INNER) {GmItem.goodMovementId eq GoodMovement.id }
            .join(Sku, JoinType.INNER) { Sku.id eq GmItem.skuId }
            .join(LotNo, JoinType.INNER) { LotNo.id eq GmItem.lotNoId }
            .join(StoreLocation, JoinType.INNER) { StoreLocation.id eq GmItem.storeLocationId }
            .join(BookedLog, JoinType.LEFT) { (BookedLog.lotNoId eq LotNo.id) and (BookedLog.goodMovementId eq GoodMovement.id) and (BookedLog.status eq "A") }
        val query = joins.select(Sku.matCode, GmItem.qty, Sku.volumnM3, Sku.areaM2, GmItem.createdAt, LotNo.refCode,
                        LotNo.extraAttributes, StoreLocation.name, LotNo.id, BookedLog.lotNoId)
            .where{ (GmItem.goodMovementId eq goodMovementId) and (LotNo.status eq "A") and (GmItem.status eq "A") }
        return query.map { row ->
            val extraAttributes = row[LotNo.extraAttributes]
            GetLogDetailsResponse.Log(
                matCode = row[Sku.matCode],
                qty = row[GmItem.qty].toInt(),
                volumnM3 = row[LotNo.extraAttributes]?.get("volumnM3")?.toBigDecimal() ?: row[Sku.volumnM3] ,
                areaM2 = row[Sku.areaM2],
                recordedAt = RECORED_DATETIME_FORMAT.format(row[GmItem.createdAt]),
                barcode = row[LotNo.refCode],
                logNo = row[LotNo.extraAttributes]?.get("logNo") ?: "",
                location = row[StoreLocation.name],
                lotNoId = row[LotNo.id].value,
                isBooked = row[BookedLog.lotNoId] != null,
                supplier = extraAttributes?.get("supplier") ?: "",
                forestryBook = extraAttributes?.get("forestryBook") ?: "",
                forestryBookNo = extraAttributes?.get("forestryBookNo") ?: ""
            )
        }
    }

    private fun getUnPickedLogs(lotNoIds: List<Int>, goodMovementId: Int): List<GetLogDetailsResponse.UnpickedLog> {
        val joins = BookedLog.join(LotNo, JoinType.INNER) { LotNo.id eq BookedLog.lotNoId }
            .join(GmItem, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(GoodMovement, JoinType.INNER) { GmItem.goodMovementId eq GoodMovement.id }
            .join(Sku, JoinType.INNER) { Sku.id eq GmItem.skuId }
            .join(StoreLocation, JoinType.INNER) { StoreLocation.id eq GmItem.storeLocationId }
        val query = joins.select(Sku.matCode, GmItem.qty, Sku.volumnM3, Sku.areaM2, GmItem.createdAt, LotNo.refCode,
            LotNo.extraAttributes, StoreLocation.name, LotNo.id)
            .where{ (BookedLog.lotNoId notInList lotNoIds) and (LotNo.status eq "A") and (GmItem.status eq "A") and
                (BookedLog.goodMovementId eq goodMovementId) and (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName)}

        return query.map { row ->
            val extraAttributes = row[LotNo.extraAttributes]
            GetLogDetailsResponse.UnpickedLog(
                matCode = row[Sku.matCode],
                volumnM3 = row[LotNo.extraAttributes]?.get("volumnM3")?.toBigDecimal() ?: row[Sku.volumnM3],
                areaM2 = row[Sku.areaM2],
                barcode = row[LotNo.refCode],
                logNo = row[LotNo.extraAttributes]?.get("logNo") ?: "",
                location = row[StoreLocation.name],
                lotNoId = row[LotNo.id].value,
                supplier = extraAttributes?.get("supplier") ?: "",
                forestryBook = extraAttributes?.get("forestryBook") ?: "",
                forestryBookNo = extraAttributes?.get("forestryBookNo") ?: ""
            )
        }
    }
}