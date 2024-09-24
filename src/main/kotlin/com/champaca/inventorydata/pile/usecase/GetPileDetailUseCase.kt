package com.champaca.inventorydata.pile.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.storelocation.StoreLocationRepository
import com.champaca.inventorydata.masterdata.supplier.SupplierRepository
import com.champaca.inventorydata.masterdata.user.UserRepository
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.response.GetPileDetailResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import javax.sql.DataSource

@Service
class GetPileDetailUseCase(
    val dataSource: DataSource,
    val pileService: PileService,
    val userRepository: UserRepository,
    val storeLocationRepository: StoreLocationRepository,
    val supplierRepository: SupplierRepository
) {
    companion object {
        val MAIN_PILE_REFCODE = "[A-Z0-9]{2}[0-9]{8}_[A-Z0-9]{2}[0-9]{2}".toRegex()
        val RM_DEPARTMENT_ID = 7
        val FLOORING_DEPARTMENT_ID = 9
        val PRODUCTION_DEPARTMENTS = listOf(RM_DEPARTMENT_ID, FLOORING_DEPARTMENT_ID)
    }

    val logger = LoggerFactory.getLogger(GetPileDetailUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(pileCode: String): GetPileDetailResponse {
        Database.connect(dataSource)

        var errorType = PileError.NONE
        var status = ""
        var items: List<GetPileDetailResponse.Item> = listOf()
        var timelines: List<GetPileDetailResponse.Timeline> = listOf()
        lateinit var goodMovement: GoodMovementDao
        var currentLocation = ""
        lateinit var pile: PileDao
        transaction {
            addLogger(exposedLogger)
            val pair = pileService.findPileAndCurrentLotNos(pileCode, needToBeActive = false)
            if (pair == null) {
                errorType = PileError.PILE_NOT_FOUND
                logger.warn("Pile: ${pileCode} not found")
                return@transaction
            }

            pile = pair.first
            val lotNoIds = pair.second.map { it.id.value }
            goodMovement = pile.goodMovement
            if (lotNoIds.isNotEmpty()) {
                items = if (pile.type != PileDao.SHELF) {
                    composeItems(pile, goodMovement, lotNoIds)
                } else {
                    composeItemsForShelf(pile, goodMovement, lotNoIds)
                }
            }
            timelines = composeTimeline(pile.id.value)
            if (goodMovement.type == GoodMovementType.GOODS_RECEIPT.wmsName) {
                currentLocation = getLocation(pile.storeLocationId)
            }

            status = getStatus(pile, items, goodMovement, currentLocation)
        }

        return if (errorType != PileError.NONE) {
            GetPileDetailResponse.Failure(errorType = errorType)
        } else {
            GetPileDetailResponse.Success(
                status = status,
                goodMovementId = goodMovement.id.value,
                jobNo = goodMovement.jobNo,
                items = items,
                totalInitial = items.sumOf { it.initialQty },
                totalCurrent = items.sumOf { it.currentQty },
                timelines = timelines,
                currentLocation = currentLocation,
                orderNo = pile.orderNo,
                remark = pile.remark,
            )
        }
    }

    private fun getStatus(pile: PileDao, items: List<GetPileDetailResponse.Item>, goodMovement: GoodMovementDao, currentLocation: String): String {
        if (!pile.isActive()) {
            return "กองไม้ถูกลบออกจากระบบ"
        }
        if(items.all { it.currentQty == BigDecimal.ZERO }) {
            return "Item ในกองถูกเบิกหมดแล้ว"
        }
        if (goodMovement.type == GoodMovementType.GOODS_RECEIPT.wmsName) {
            return "กองอยู่ที่ตำแหน่ง ${currentLocation}"
        }
        val gmIdToProcessMap = getGoodMovementAndProcessType(listOf(goodMovement.id.value))
        val processName = gmIdToProcessMap[goodMovement.id.value]!!.first
        val manuName = gmIdToProcessMap[goodMovement.id.value]!!.second
        return "กองอยู่ในการผลิตขั้นตอน $processName เครื่อง $manuName"
    }

    private fun getLocation(storeLocationId: Int): String {
        return StoreLocationDao.findById(storeLocationId)!!.code
    }

    private fun composeTimeline(pileId: Int): List<GetPileDetailResponse.Timeline> {
        val transactionTimeline = composeTransactionTimeline(pileId)
        val relocationTimeline = composeRelocationTimeline(pileId)
        return (transactionTimeline + relocationTimeline).sortedBy { it.localDateTime }
    }

    private fun composeTransactionTimeline(pileId: Int): List<GetPileDetailResponse.Timeline> {
        val transactions = PileTransactionDao.find { PileTransaction.pileId eq pileId }.toList()
        val goodMovementIds = (transactions.filter { it.toGoodMovementId != null }.map { it.toGoodMovementId!! } +
                transactions.filter { it.fromGoodMovementId != null }.map { it.fromGoodMovementId!! }).toSet().toList()
        val gmIdToProcessMap = getGoodMovementAndProcessType(goodMovementIds)
        val goodMovementMap = GoodMovementDao.find { GoodMovement.id.inList(goodMovementIds) }.associateBy({it.id.value}, {it})
        val toPileIds = transactions.filter { it.toPileId != null }.map { it.toPileId!! }
        val pilesById = PileDao.find { Pile.id.inList(toPileIds) }.associateBy({it.id.value}, {it})
        val transactionTimeline = transactions.map { tx ->
            val description = when(tx.type) {
                PileTransactionDao.CREATE -> {
                    val goodMovement = goodMovementMap[tx.toGoodMovementId]!!
                    "ตั้งกอง ${suffix(goodMovement)}"
                }
                PileTransactionDao.PICK -> {
                    val goodMovement = goodMovementMap[tx.toGoodMovementId]!!
                    val suffix = suffix(goodMovement)
                    if (goodMovement.manufacturingLineId == null) {
                        val supplier = supplierRepository.findById(goodMovement.supplierId!!)
                        "เบิกส่ง${supplier!!.name} $suffix"
                    } else {
                        val processName = gmIdToProcessMap[tx.toGoodMovementId]!!.first
                        val manuName = gmIdToProcessMap[tx.toGoodMovementId]!!.second
                        "เบิกเข้าผลิตขั้นตอน ${processName} เครื่อง $manuName $suffix"
                    }
                }
                PileTransactionDao.PARTIAL_PICK -> {
                    val goodMovement = goodMovementMap[tx.toGoodMovementId]!!
                    val suffix = suffix(goodMovement)
                    if (goodMovement.manufacturingLineId == null) {
                        val supplier = supplierRepository.findById(goodMovement.supplierId!!)
                        "เบิกบางส่วนส่ง${supplier!!.name} จำนวน ${tx.movingQty} คงเหลือ ${tx.remainingQty} $suffix"
                    } else {
                        "เบิกบางส่วนจำนวน ${tx.movingQty} คงเหลือ ${tx.remainingQty} เข้า $suffix"
                    }
                }
                PileTransactionDao.RECEIVE -> {
                    val goodMovement = goodMovementMap[tx.toGoodMovementId]!!
                    val processName = gmIdToProcessMap[tx.toGoodMovementId]!!.first
                    val manuName = gmIdToProcessMap[tx.toGoodMovementId]!!.second
                    val suffix = suffix(goodMovement)
                    "รับออกจากการผลิตขั้นตอน ${processName} เครื่อง $manuName $suffix"
                }
                PileTransactionDao.EDIT -> {
                    "แก้ไขจำนวน เหตุผล: ${tx.remark}"
                }
                PileTransactionDao.REMOVE -> {
                    "ลบกอง เหตุผล: ${tx.remark}"
                }
                PileTransactionDao.UNDO -> {
                    "Undo เหตุผล: ${tx.remark}"
                }
                PileTransactionDao.RECEIVE_TRANSFER -> {
                    val (qtyList, sum) = splitAndSum(tx.fromPile, tx.movingQty)
                    "รับโอนไม้จาก ${qtyList} รวม ${sum}"
                }
                PileTransactionDao.ASSEMBLE -> {
                    "ตั้งกองใหม่ด้วยการรวมไม้จากกอง ${splitAndComma(tx.fromPile)}"
                }
                PileTransactionDao.PICK_FOR_ASSEMBLE -> {
                    val pile = pilesById[tx.toPileId]!!
                    val movingQtys = tx.movingQty!!.split(",").map { it.toInt() }
                    "เบิกเพื่อไปตั้งกองใหม่ ${pile.code} จำนวน ${movingQtys.sum()}"
                }
                else -> { "" }
            }
            val datetime = tx.createdAt
            val user = userRepository.findById(tx.userId)!!.let { "${it.firstname} ${it.lastname}" }
            GetPileDetailResponse.Timeline(description, datetime, user, tx.undone)
        }
        return transactionTimeline
    }

    private fun suffix(goodMovement: GoodMovementDao): String {
        return when {
            !goodMovement.jobNo.isNullOrEmpty() -> " Job:${goodMovement.jobNo}"
            else -> ""
        }
    }

    private fun composeRelocationTimeline(pileId: Int): List<GetPileDetailResponse.Timeline> {
        val relocations = PileRelocationDao.find { PileRelocation.pileId eq pileId }.toList()
        val relocationTimeline = relocations.map {
            val fromLocation = storeLocationRepository.getById(it.fromStoreLocationId)
            val toLocation = storeLocationRepository.getById(it.toStoreLocationId)
            val description = "ย้ายจาก ${fromLocation!!.name} ไปยัง ${toLocation!!.name}"
            val datetime = it.createdAt
            val username = userRepository.findById(it.userId)!!.let { "${it.firstname} ${it.lastname}" }
            GetPileDetailResponse.Timeline(description, datetime, username, false)
        }
        return relocationTimeline
    }

    private fun getGoodMovementAndProcessType(goodMovementIds: List<Int>): Map<Int, Pair<String, String>> {
        val joins = GoodMovement.join(ManufacturingLine, JoinType.INNER) { GoodMovement.manufacturingLineId eq ManufacturingLine.id }
            .join(ProcessType, JoinType.INNER) { ProcessType.id eq ManufacturingLine.processTypeId }
        val query = joins.select(GoodMovement.id, ManufacturingLine.name, ProcessType.name)
            .where { GoodMovement.id.inList(goodMovementIds) }
        return query.map { resultRow ->
            Pair(resultRow[GoodMovement.id].value, Pair(resultRow[ProcessType.name], resultRow[ManufacturingLine.name]))
        }.associateBy({it.first}, {it.second})
    }

    private fun composeItems(pile: PileDao, goodMovement: GoodMovementDao, lotNoIds: List<Int>): List<GetPileDetailResponse.Item> {
        val initialItems = pileService.findPileInitialItems(pile)
        val currentItems = when(goodMovement.type) {
            GoodMovementType.GOODS_RECEIPT.wmsName -> pileService.findItemsInStorageArea(lotNoIds)
            GoodMovementType.PICKING_ORDER.wmsName -> {
                if (goodMovement.manufacturingLineId != null) {
                    pileService.findItemsInProcess(lotNoIds, goodMovement.manufacturingLineId!!)
                } else {
                    listOf()
                }
            }
            else -> listOf()
        }

        if (isComplicatedProductionPile(pile, goodMovement)) {
            val currentOrderMap = currentItems.groupBy { it.lotRefCode.takeLast(2) }
            return initialItems.map {
                val currentEntries = currentOrderMap[it.lotRefCode.takeLast(2)]
                    ?: return@map GetPileDetailResponse.Item(
                        initialMatCode = it.matCode,
                        currentMatCode = "NA",
                        skuName = it.skuName,
                        initialQty = it.qty,
                        currentQty = 0.toBigDecimal()
                    )
                val currentQty = currentEntries.sumOf { it.qty }
                val currentMatCode = currentEntries.first { it.lotRefCode.matches(MAIN_PILE_REFCODE) }.matCode
                GetPileDetailResponse.Item(
                    initialMatCode = it.matCode,
                    currentMatCode = currentMatCode,
                    skuName = it.skuName,
                    initialQty = it.qty,
                    currentQty = currentQty
                )
            }
        } else {
            val currentItemMap = currentItems.associateBy({ it.matCode.substring(3) }, { Pair(it.matCode, it.qty) })
            return initialItems.map {
                val skuDimension = it.matCode.substring(3)
                val currentEntry = currentItemMap[skuDimension]
                val currentQty = currentEntry?.second ?: 0.toBigDecimal()
                GetPileDetailResponse.Item(
                    initialMatCode = it.matCode,
                    currentMatCode = currentEntry?.first ?: "NA",
                    skuName = it.skuName,
                    initialQty = it.qty,
                    currentQty = currentQty
                )
            }
        }
    }

    private fun isComplicatedProductionPile(pile: PileDao, goodMovement: GoodMovementDao): Boolean {
        return goodMovement.departmentId.value in PRODUCTION_DEPARTMENTS && pile.lotSet > 1
    }

    private fun composeItemsForShelf(pile: PileDao, goodMovement: GoodMovementDao, lotNoIds: List<Int>): List<GetPileDetailResponse.Item> {
        val itemsByMatCode = pileService.findItemsInStorageArea(lotNoIds).groupBy { it.matCode }
        return itemsByMatCode.map { (matCode, items) ->
            GetPileDetailResponse.Item(
                initialMatCode = matCode,
                currentMatCode = matCode,
                skuName = items.first().skuName,
                initialQty = items.sumOf { it.qty },
                currentQty = items.sumOf { it.qty }
            )
        }
    }

    private fun splitAndComma(str: String?): String {
        return str?.split(",")?.joinToString(", ") ?: ""
    }

    private fun splitAndSum(pilesStr: String?, qtysStr: String?): Pair<String, Int> {
        if (pilesStr == null || qtysStr == null) {
            return Pair("", 0)
        }
        var piles = pilesStr.split(",")
        val qtys = qtysStr.split(",").map { it.toDouble() }
        piles = piles.mapIndexed { index, pile -> "$pile(${qtys[index]})" }
        return Pair(piles.joinToString(", "), qtys.sum().toInt())
    }
}