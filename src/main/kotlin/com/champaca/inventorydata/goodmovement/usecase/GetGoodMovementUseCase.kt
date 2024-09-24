package com.champaca.inventorydata.goodmovement.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementData
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.goodmovement.request.GetGoodMovementRequest
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.json.contains
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class GetGoodMovementUseCase(
    val dataSource: DataSource,
    val wmsService: WmsService
) {

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val logger = LoggerFactory.getLogger(GetGoodMovementUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, request: GetGoodMovementRequest): List<GoodMovementData> {
        if (request.isBlank()) {
            logger.warn("Blank request. This seems like a rogue request.")
            return emptyList()
        }

        Database.connect(dataSource)
        var results = listOf<GoodMovementData>()
        transaction {
            addLogger(exposedLogger)
            results = getGoodMovement(request)
            val goodMovementIds = results.map { it.id }
            val goodMovementIdToQtyMap = getItemCount(goodMovementIds)
            val goodMovementIdToPileCountMap = getPileCount(goodMovementIds)
            results.forEach { goodMovementData ->
                val resultRow = goodMovementIdToQtyMap[goodMovementData.id]
                if (resultRow != null) {
                    goodMovementData.itemCount = resultRow.map { it[GmItem.qty] }.sumOf { it.toInt() }
                    goodMovementData.totalVolumnFt3 = resultRow.sumOf { it[Sku.volumnFt3] * it[GmItem.qty] }.setScale(2, RoundingMode.HALF_UP)
                    goodMovementData.totalVolumnM3 = resultRow.sumOf { it[Sku.volumnM3] * it[GmItem.qty] }.setScale(2, RoundingMode.HALF_UP)
                    goodMovementData.totalAreaM2 = resultRow.sumOf {
                        if (it[Sku.areaM2] != null) {
                            it[Sku.areaM2]!! * it[GmItem.qty]
                        } else {
                            BigDecimal.ZERO
                        }
                    }.setScale(2, RoundingMode.HALF_UP)
                }
                goodMovementData.pileCount = goodMovementIdToPileCountMap[goodMovementData.id] ?: 0
            }
        }

        return results
    }

    private fun getItemCount(goodMovementIds: List<Int>): Map<Int, List<ResultRow>> {
        val joins = GmItem.join(GoodMovement, JoinType.INNER) { GmItem.goodMovementId eq GoodMovement.id }
            .join(Sku, JoinType.INNER) { GmItem.skuId eq Sku.id }
        val query = joins.select(GoodMovement.id, GmItem.qty, Sku.volumnFt3, Sku.volumnM3, Sku.areaM2)
            .where { (GmItem.status eq "A") and (GoodMovement.id.inList(goodMovementIds)) }
        return query.toList().groupBy { it[GoodMovement.id].value }
    }

    private fun getPileCount(goodMovementIds: List<Int>): Map<Int, Int> {
        val joins = PileHasLotNo.join(LotNo, JoinType.INNER) { PileHasLotNo.lotNoId eq LotNo.id }
            .join(GmItem, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(GoodMovement, JoinType.INNER) { GoodMovement.id eq GmItem.goodMovementId }
        val query = joins.select(GoodMovement.id, PileHasLotNo.pileId.countDistinct())
            .where { (GmItem.status eq "A") and (LotNo.status eq "A") and (GoodMovement.id.inList(goodMovementIds)) }
            .groupBy(GoodMovement.id)
        return query.map { resultRow -> Pair(resultRow[GoodMovement.id].value, resultRow[PileHasLotNo.pileId.countDistinct()]) }
            .associateBy({it.first}, {it.second.toInt()})
    }

    private fun getGoodMovement(request: GetGoodMovementRequest): List<GoodMovementData> {
        val approvingUser = User.alias("approvingUser")
        val creatingUser = User.alias("creatingUser")
        val joins = GoodMovement.join(ManufacturingLine, JoinType.LEFT) { GoodMovement.manufacturingLineId eq ManufacturingLine.id }
            .join(ProcessType, JoinType.LEFT) { ProcessType.id eq ManufacturingLine.processTypeId }
            .join(approvingUser, JoinType.LEFT) { approvingUser[User.id] eq GoodMovement.approveUserId }
            .join(creatingUser, JoinType.INNER) { creatingUser[User.id] eq GoodMovement.userId }
            .join(Supplier, JoinType.LEFT) { Supplier.id eq GoodMovement.supplierId }
            .join(Department, JoinType.LEFT) { Department.id eq GoodMovement.departmentId }
        val query = joins.select(GoodMovement.id, GoodMovement.code, GoodMovement.type, ProcessType.name, ProcessType.id,
                    ManufacturingLine.name, ManufacturingLine.id, GoodMovement.lotNo, GoodMovement.supplierId,
                    GoodMovement.poNo, GoodMovement.orderNo, GoodMovement.jobNo, GoodMovement.invoiceNo,
                    creatingUser[User.firstname], creatingUser[User.lastname], approvingUser[User.firstname], approvingUser[User.lastname],
                    GoodMovement.productionDate, Supplier.name, Supplier.id, Department.id, Department.name, GoodMovement.remark,
                    GoodMovement.extraAttributes, GoodMovement.productType)
            .where { (GoodMovement.status eq "A") and (GoodMovement.type eq request.type.wmsName)}
            .orderBy(GoodMovement.id to SortOrder.DESC)

        if (request.processTypeId != null) {
            query.andWhere { ProcessType.id eq request.processTypeId }
        }
        if (request.manufacturingLineId != null) {
            query.andWhere { GoodMovement.manufacturingLineId eq request.manufacturingLineId }
        }
        if (!request.fromProductionDate.isNullOrEmpty()) {
            query.andWhere { GoodMovement.productionDate greaterEq LocalDate.parse(request.fromProductionDate, DATE_FORMAT) }
        } else {
            // อันนี้ใส่เพื่อป้องกันการเรียก API นี้แบบ Random ทำให้เรียกข้อมูลเยอะมาก ยังหาสาเหตุไม่เจอว่าเรียกมาจากไหน
            val lastMonth = LocalDate.now().minusMonths(1.toLong())
            query.andWhere { GoodMovement.productionDate greaterEq lastMonth }
            logger.warn("Blank request. This seems like a rogue request!!!")
        }
        if (!request.toProductionDate.isNullOrEmpty()) {
            query.andWhere { GoodMovement.productionDate lessEq LocalDate.parse(request.toProductionDate, DATE_FORMAT) }
        }
        if (request.supplierId != null) {
            query.andWhere { GoodMovement.supplierId eq request.supplierId }
        }
        if (request.departmentId != null) {
            query.andWhere { GoodMovement.departmentId eq request.departmentId }
        }
        if (!request.jobNo.isNullOrEmpty()) {
            query.andWhere { GoodMovement.jobNo eq request.jobNo }
        }
        if (!request.orderNo.isNullOrEmpty()) {
            query.andWhere { GoodMovement.orderNo eq request.orderNo }
        }
        if (request.hasReference != null) {
            if (request.hasReference) {
                query.andWhere { GoodMovement.goodReceiptGoodMovementId.isNotNull() }
            } else {
                query.andWhere { GoodMovement.goodReceiptGoodMovementId.isNull() }
            }
        }
        if (!request.purpose.isNullOrEmpty()) {
            query.andWhere { GoodMovement.extraAttributes.contains("{\"purpose\":\"${request.purpose}\"}") }
        }

        return query.map { resultRow ->
            val approvedBy = if (resultRow[approvingUser[User.firstname]].isNullOrEmpty()) ""
                else "${resultRow[approvingUser[User.firstname]]} ${resultRow[approvingUser[User.lastname]]}"
            val createdBy = "${resultRow[creatingUser[User.firstname]]} ${resultRow[creatingUser[User.lastname]]}"
            GoodMovementData(
                id = resultRow[GoodMovement.id].value,
                code = resultRow[GoodMovement.code],
                type = GoodMovementType.fromString(resultRow[GoodMovement.type]),
                processType = resultRow[ProcessType.name] ?: "",
                processTypeId = resultRow[ProcessType.id]?.value ?: -1,
                manufacturingLine = resultRow[ManufacturingLine.name] ?: "",
                manufacturingLineId = resultRow[ManufacturingLine.id]?.value ?: -1,
                departmentId = resultRow[Department.id]?.value ?: 0,
                department = resultRow[Department.name] ?: "",
                productionDate = resultRow[GoodMovement.productionDate].format(DATE_FORMAT),
                orderNo = resultRow[GoodMovement.orderNo],
                jobNo = resultRow[GoodMovement.jobNo],
                poNo = resultRow[GoodMovement.poNo],
                invoiceNo = resultRow[GoodMovement.invoiceNo],
                lotNo = resultRow[GoodMovement.lotNo],
                supplierId = resultRow[Supplier.id]?.value,
                supplier = resultRow[Supplier.name] ?: "",
                createdBy = createdBy,
                approvedBy = approvedBy,
                remark = resultRow[GoodMovement.remark],
                extraAttributes = resultRow[GoodMovement.extraAttributes],
                productType = resultRow[GoodMovement.productType]
            )
        }
    }
}