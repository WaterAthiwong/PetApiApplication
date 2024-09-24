package com.champaca.inventorydata.odoo.usecase.out

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.common.ChampacaConstant.VOLUMN_M3
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.SupplierDao.Companion.INTERNAL
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.odoo.OdooService
import com.champaca.inventorydata.odoo.model.out.CreateStockPickingRequest
import com.champaca.inventorydata.odoo.request.out.StockMovementRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class StockMovementUseCase(
    val dataSource: DataSource,
    val odooService: OdooService
) {
    companion object {
        val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val RELOCATION_TYPE = "RE"
        val GOOD_MOVEMENT_TYPE = "GM"
    }

    @Value("\${odoo.stock.location.logYard.id}")
    lateinit var logYardLocationId: String

    @Value("\${odoo.stock.location.warehouse.id}")
    lateinit var warehouseLocationId: String

    @Value("\${odoo.stock.location.conditionRoom.id}")
    lateinit var condRoomLocationId: String

    @Value("\${odoo.stock.location.warehouseFg.id}")
    lateinit var warehouseFgLocationId: String

    @Value("\${odoo.stock.location.production.id}")
    lateinit var productionLocationId: String

    @Value("\${odoo.stock.pickingType.incoming.warehouse.id}")
    lateinit var incomingWarehouseId: String

    @Value("\${odoo.stock.pickingType.incoming.conditionRoom.id}")
    lateinit var incomingConditionRoomId: String

    @Value("\${odoo.stock.pickingType.incoming.warehouseFg.id}")
    lateinit var incomingWarehouseFgId: String

    @Value("\${odoo.stock.pickingType.outgoing.logYard.id}")
    lateinit var outgoingLogYardId: String

    @Value("\${odoo.stock.pickingType.outgoing.warehouse.id}")
    lateinit var outgoingWarehouseId: String

    @Value("\${odoo.stock.pickingType.outgoing.conditionRoom.id}")
    lateinit var outgoingConditionRoomId: String

    val items = listOf(13778, 13779, 13780, 13781, 13782) // TODO - remove after test

    fun getData(request: StockMovementRequest) {
        Database.connect(dataSource)

        transaction {
            addLogger(ExposedInfoLogger)
        }
    }

    fun sendToOdoo(request: StockMovementRequest): List<CreateStockPickingRequest> {
        Database.connect(dataSource)

        var requests = listOf<CreateStockPickingRequest>()
        transaction {
            addLogger(ExposedInfoLogger)

            if (request.departmentId == 1) {
                val moveLines = getLogPicking(request)
                val request = createPickRequest(request, true, moveLines)
                requests = listOf(request)
            } else {
                val goodMovements = getInternalCompanyMovement(request)
                val incomingGoodMovements =
                    goodMovements.filter { it[GoodMovement.type] == GoodMovementType.GOODS_RECEIPT.wmsName }
                val outgoingGoodMovements =
                    goodMovements.filter { it[GoodMovement.type] == GoodMovementType.PICKING_ORDER.wmsName }

                val inMoveLines = createMoveLines(RELOCATION_TYPE, getMoveInStock(request)) + createMoveLines(
                    GOOD_MOVEMENT_TYPE,
                    incomingGoodMovements
                )
                val inRequest = createPickRequest(request, false, inMoveLines)

                val outMoveLines = createMoveLines(RELOCATION_TYPE, getMoveOutStock(request)) + createMoveLines(
                    GOOD_MOVEMENT_TYPE,
                    outgoingGoodMovements
                )
                val outRequest = createPickRequest(request, true, outMoveLines)
                requests = listOf(inRequest, outRequest)
            }
        }
//        requests.forEach {
//            odooService.pickStock(it)
//        }

        return requests
    }

    private fun createMoveLines(movementType: String, movedStock: List<ResultRow>): List<CreateStockPickingRequest.MoveLine> {
        return movedStock.map {
            val randomId = it[Sku.id].value % 5 // TODO - remove after test
            val movementReference = if (movementType == RELOCATION_TYPE) it[PileRelocation.id].value else it[GoodMovement.id].value
            CreateStockPickingRequest.MoveLine(
                productId = items[randomId], // TODO - change to it[Sku.erpCode]!!.toInt()
//                productId = it[Sku.erpCode]!!.toInt(),
                productUomQty = it[GmItem.qty],
                packNo = it[Pile.code],
                wmsRefNo = "${movementType}${movementReference}:${it[LotNo.refCode]}"
            )
        }
    }

    private fun createPickRequest(request: StockMovementRequest, isPicking: Boolean, moveLines: List<CreateStockPickingRequest.MoveLine>): CreateStockPickingRequest {
        return CreateStockPickingRequest(
            dateDone = LocalDateTime.now().format(DATETIME_FORMAT),
            locationId = if (isPicking) findLocationId(request.departmentId) else productionLocationId.toInt(),
            locationDestinationId = if (isPicking) productionLocationId.toInt() else findLocationId(request.departmentId),
            pickingTypeId = findPickingTypeId(request.departmentId, isPicking),
            moveLines = moveLines
        )
    }

    private fun findLocationId(departmentId: Int): Int {
        return when(departmentId) {
            1 -> logYardLocationId.toInt()
            5 -> warehouseLocationId.toInt()
            6 -> condRoomLocationId.toInt()
            10 -> warehouseFgLocationId.toInt()
            else -> 0
        }
    }

    private fun findPickingTypeId(departmentId: Int, isPicking: Boolean): Int {
        return when(departmentId) {
            1 -> if (isPicking) outgoingLogYardId.toInt() else 0
            5 -> if (isPicking) outgoingWarehouseId.toInt() else incomingWarehouseId.toInt()
            6 -> if (isPicking) outgoingConditionRoomId.toInt() else incomingConditionRoomId.toInt()
            10 -> if (isPicking) 0 else incomingWarehouseFgId.toInt()
            else -> 0
        }
    }

    private fun getInternalCompanyMovement(request: StockMovementRequest): List<ResultRow> {
        // มี 2 กรณีคือรับไม้เข้าจากการคืน (โดยเช็คว่า supplier เป็น internal) และกรณีเบิกไม้ส่งไปแผนกอื่นๆ
        val joins = GoodMovement.join(GmItem, JoinType.INNER) { (GoodMovement.id eq GmItem.goodMovementId) }
            .join(Sku, JoinType.INNER) { GmItem.skuId eq Sku.id }
            .join(LotNo, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(PileHasLotNo, JoinType.INNER) { LotNo.id eq PileHasLotNo.lotNoId }
            .join(Pile, JoinType.INNER) { (PileHasLotNo.pileId eq Pile.id) }
            .join(Supplier, JoinType.INNER) { Supplier.id eq GoodMovement.supplierId }
        val query = joins.select(
            GoodMovement.id,
            GoodMovement.type,
            GoodMovement.jobNo,
            GmItem.qty,
            Sku.id,
            Sku.erpCode,
            Sku.matCode,
            Pile.code,
            LotNo.refCode
        )
            .where { (GmItem.createdAt.date() eq LocalDate.parse(request.movedDate, DATE_FORMAT)) and
                    (GoodMovement.departmentId eq request.departmentId) and (Supplier.type eq INTERNAL) and
                    (LotNo.status eq "A") and (GmItem.status eq "A") }
        return query.toList()
    }

    private fun getMoveInStock(request: StockMovementRequest): List<ResultRow> {
        val fromLocation = StoreLocation.alias("fromLocation")
        val fromStoreZone = StoreZone.alias("fromStoreZone")
        val toLocation = StoreLocation.alias("toLocation")
        val toStoreZone = StoreZone.alias("toStoreZone")
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
        val query = joins.select(
            PileRelocation.id,
            GmItem.qty,
            Sku.id,
            Sku.matCode,
            Sku.erpCode,
            Pile.code,
            LotNo.refCode
        )
        .where {
            (Pile.status eq "A") and
                    (fromLocation[StoreLocation.status] eq "A") and
                    (toLocation[StoreLocation.status] eq "A") and
                    (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) and
                    (GmItem.status eq "A") and
                    (toStoreZone[StoreZone.departmentId] eq request.departmentId) and
                    (fromStoreZone[StoreZone.departmentId] neq request.departmentId) and
                    (PileRelocation.createdAt.date() eq LocalDate.parse(request.movedDate, DATE_FORMAT))
        }
        return query.toList()
    }

    private fun getMoveOutStock(request: StockMovementRequest): List<ResultRow> {
        val fromLocation = StoreLocation.alias("fromLocation")
        val fromStoreZone = StoreZone.alias("fromStoreZone")
        val toLocation = StoreLocation.alias("toLocation")
        val toStoreZone = StoreZone.alias("toStoreZone")
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
        val query = joins.select(
            PileRelocation.id,
            GmItem.qty,
            Sku.id,
            Sku.matCode,
            Sku.erpCode,
            Pile.code,
            LotNo.refCode
        )
            .where {
                (Pile.status eq "A") and
                        (fromLocation[StoreLocation.status] eq "A") and
                        (toLocation[StoreLocation.status] eq "A") and
                        (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) and
                        (GmItem.status eq "A") and
                        (toStoreZone[StoreZone.departmentId] neq request.departmentId) and
                        (fromStoreZone[StoreZone.departmentId] eq request.departmentId) and
                        (PileRelocation.createdAt.date() eq LocalDate.parse(request.movedDate, DATE_FORMAT))
            }
        return query.toList()
    }

    private fun getLogPicking(request: StockMovementRequest): List<CreateStockPickingRequest.MoveLine> {
        val joins = GoodMovement.join(GmItem, JoinType.INNER) { GoodMovement.id eq GmItem.goodMovementId }
            .join(LotNo, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(Sku, JoinType.INNER) { GmItem.skuId eq Sku.id }
        val query = joins.select(
            Sku.erpCode,
            LotNo.refCode,
            LotNo.extraAttributes
        )
            .where {
                (GmItem.createdAt.date() eq LocalDate.parse(request.movedDate, DATE_FORMAT)) and
                (GoodMovement.departmentId eq request.departmentId) and
                (GoodMovement.type eq GoodMovementType.PICKING_ORDER.wmsName) and
                (LotNo.status eq "A") and (GmItem.status eq "A")
            }
        return query.map {
            val extraAttributes = it[LotNo.extraAttributes]!!
            val wmsRefNo = "${GOOD_MOVEMENT_TYPE}${it[GoodMovement.id].value}:${it[LotNo.refCode]}"
            CreateStockPickingRequest.MoveLine(
                productId = it[Sku.erpCode]!!.toInt(),
                productUomQty = extraAttributes[VOLUMN_M3]!!.toBigDecimal(),
                lotName = it[LotNo.refCode],
                packNo = it[LotNo.refCode],
                wmsRefNo = wmsRefNo
            )
        }
    }
}