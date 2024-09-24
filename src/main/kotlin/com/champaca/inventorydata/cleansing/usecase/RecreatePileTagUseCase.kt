package com.champaca.inventorydata.cleansing.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.config.ConfigRepository
import com.champaca.inventorydata.pile.PileService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Deprecated("Use for import existing data only")
@Service
class RecreatePileTagUseCase(
    val dataSource: DataSource,
    val configRepository: ConfigRepository,
    val pileService: PileService
) {
    val IMO_FORMAT = "IMO\\d{7}-\\d{4}".toRegex()

    fun execute(lotGroupCodes: List<String>): List<String> {
        Database.connect(dataSource)

        var pileCodes = mutableListOf<String>()
        transaction {
            addLogger(ExposedInfoLogger)

            lotGroupCodes.forEach { lotGroupCode ->
//                println("LotGroup: ${lotGroupCode}")
                val lotNos = findLotNo(lotGroupCode)
//                println(lotNos.map { it.refCode }.joinToString("\n"))
                val gmItems = findGmItems(lotNos.map { it.id.value })
//                println(gmItems.map { "${it.id.value}: ${it.goodMovementId}: ${it.qty}" }.joinToString("\n"))
                val goodMovementId = gmItems.first().goodMovementId
                val refCode = lotNos.first().refCode
                val pileCode = if (refCode.matches(IMO_FORMAT)) {
                    createPile(lotNos, "SM", goodMovementId)
                } else {
                    refCode.substringBefore('_')
                }
                pileCodes.add(pileCode)
            }
        }
        return pileCodes
    }

    private fun findLotNo(lotGroupCode: String): List<LotNoDao> {
        val joins = LotGroup.join(LotNo, JoinType.INNER) { LotGroup.id eq LotNo.lotGroupId }
        val query = joins.select(LotNo.columns)
            .where { (LotGroup.code eq lotGroupCode) and (LotNo.status eq "A") }
        return LotNoDao.wrapRows(query).toList()
    }

    private fun findGmItems(lotNoIds: List<Int>): List<GmItemDao> {
        val query = GmItemDao.wrapRows(GmItemDao.table.select { GmItem.lotNoId inList lotNoIds })
        return query.toList()
    }

    private fun createPile(lotNos: List<LotNoDao>, processPrefix: String, goodMovementId: Int): String {
        val goodMovement = GoodMovementDao.findById(goodMovementId)!!
        val monthFormat = DateTimeFormatter.ofPattern("yyMM")
        val createdAt = lotNos.first().createdAt
        val prefix = "$processPrefix${createdAt.format(monthFormat)}"
        val config = configRepository.getOrPut("Pile.$prefix", defaultInt = 0)
        val nextRunning = (config.valueInt!! + 1).toString()
        val pileCode = "${prefix}${nextRunning.padStart(4, '0')}" // e.g. SM23110001, DK23120058
        config.valueInt = config.valueInt!! + 1

        val now = LocalDateTime.now()

        // Create Pile
        val pileId = Pile.insertAndGetId {
            it[this.manufacturingLineId] = goodMovement.manufacturingLineId
            it[this.goodMovementId] = goodMovementId
            it[this.originGoodMovementId] = goodMovementId
            it[this.code] = pileCode
            it[this.processTypePrefix] = "SM"
            it[this.lotSet] = 1
            it[this.type] = "woodPile"
            it[this.remark] = lotNos.first().refCode
            it[this.createdAt] = createdAt
            it[this.updatedAt] = now
            it[this.status] = "A"
        }

        val lotNoIds = lotNos.map { it.id.value }
        // Bind Pile and LotNos
        pileService.addPileHasLotNos(pileId.value, lotNoIds, 1)

        // Record pile creation transaction
        pileService.addPileTransaction(pileId = pileId.value,
            toGoodMovementId = goodMovementId,
            userId = 10,
            type = PileTransactionDao.CREATE,
            toLotNos = lotNoIds,
            remark = "คุณนัทช่วยสร้างกองไม้",
            dateTime = createdAt
        )

        return pileCode
    }

    fun refillMachine() {
        Database.connect(dataSource)

        transaction {
            addLogger(ExposedInfoLogger)
            val pileIdToSawingMap = getPileKilnDryMachine()
            val piles = PileDao.find { (Pile.id inList pileIdToSawingMap.keys) and (Pile.status eq "A") }
            piles.forEach { pile ->
                val extras = pile.extraAttributes?.toMutableMap() ?: mutableMapOf()
                extras["KD"] = pileIdToSawingMap[pile.id.value]!!
                Pile.update({ Pile.id eq pile.id }) {
                    it[this.extraAttributes] = extras
                }
            }
        }
    }

    private fun getPileSawingMachine(): Map<Int, String> {
        val joins = PileTransaction.join(Pile, JoinType.INNER) { PileTransaction.pileId eq Pile.id }
            .join(GoodMovement, JoinType.INNER) { PileTransaction.toGoodMovementId eq GoodMovement.id }
            .join(ManufacturingLine, JoinType.INNER) { GoodMovement.manufacturingLineId eq ManufacturingLine.id }
        val query = joins.slice(Pile.id, ManufacturingLine.name)
            .select { (PileTransaction.type eq PileTransactionDao.CREATE) and (ManufacturingLine.processTypeId eq 1) and
                    (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) }
        return query.map { it[Pile.id].value to it[ManufacturingLine.name] }.toMap()
    }

    private fun getPileKilnDryMachine(): Map<Int, String> {
        val joins = PileTransaction.join(Pile, JoinType.INNER) { PileTransaction.pileId eq Pile.id }
            .join(GoodMovement, JoinType.INNER) { PileTransaction.toGoodMovementId eq GoodMovement.id }
            .join(ManufacturingLine, JoinType.INNER) { GoodMovement.manufacturingLineId eq ManufacturingLine.id }
        val query = joins.slice(Pile.id, ManufacturingLine.name)
            .select { (PileTransaction.type eq PileTransactionDao.RECEIVE) and (ManufacturingLine.processTypeId eq 3) and
                    (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) }
        return query.map { it[Pile.id].value to it[ManufacturingLine.name] }.toMap()
    }
}