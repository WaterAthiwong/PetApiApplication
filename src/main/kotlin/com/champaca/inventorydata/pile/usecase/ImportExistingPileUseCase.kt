package com.champaca.inventorydata.pile.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.databasetable.GoodMovement
import com.champaca.inventorydata.databasetable.LotGroup
import com.champaca.inventorydata.databasetable.LotNo
import com.champaca.inventorydata.databasetable.Pile
import com.champaca.inventorydata.databasetable.dao.GoodMovementDao
import com.champaca.inventorydata.databasetable.dao.PileTransactionDao
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.config.ConfigRepository
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.request.ImportExistingPileRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class ImportExistingPileUseCase(
    val dataSource: DataSource,
    val pileService: PileService,
    val configRepository: ConfigRepository
) {
    fun execute(sessionId: String, userId: String, request: ImportExistingPileRequest): String {
        Database.connect(dataSource)
        val prefix = "${request.processPrefix}${request.monthPrefix}"
        var pileCode = ""
        transaction {
            addLogger(ExposedInfoLogger)
            val config = configRepository.getOrPut("Pile.$prefix", defaultInt = 0)
            val nextRunning = (config.valueInt!! + 1).toString()
            pileCode = "${prefix}${nextRunning.padStart(4, '0')}" // e.g. SM23110001, DK23120058
            config.valueInt = config.valueInt!! + 1
            val lotNoIds = getLotNoIds(request.lotGroupCode)
            val goodMovement = GoodMovementDao.find { (GoodMovement.status eq "A") and (GoodMovement.type eq GoodMovementType.GOODS_RECEIPT.wmsName) and (GoodMovement.jobNo eq request.jobNo)}.single()
            upsertPileRecords(userId, request, pileCode, lotNoIds, goodMovement)
        }
        return pileCode
    }

    private fun getLotNoIds(lotGroupCode: String): List<Int> {
        val joins = LotNo.join(LotGroup, JoinType.INNER) { LotNo.lotGroupId eq LotGroup.id }
        val query = joins.select(LotNo.id)
            .where { (LotGroup.code eq lotGroupCode) and (LotNo.status eq "A") }
        return query.map { it[LotNo.id].value }
    }

    private fun upsertPileRecords(userId: String,
                                  request: ImportExistingPileRequest,
                                  pileCode: String,
                                  lotNoIds: List<Int>,
                                  goodMovement: GoodMovementDao) {
        val now = LocalDateTime.now()

        // Create Pile
        val pileId = Pile.insertAndGetId {
            it[this.manufacturingLineId] = goodMovement.manufacturingLineId ?: -1
            it[this.goodMovementId] = goodMovement.id.value
            it[this.originGoodMovementId] = goodMovement.id.value
            it[this.code] = pileCode
            it[this.processTypePrefix] = request.processPrefix
            it[this.lotSet] = 1
            it[this.type] = request.pileType
            it[this.remark] = request.remark
            it[this.createdAt] = now
            it[this.updatedAt] = now
            it[this.status] = "A"
        }

        // Bind Pile and LotNos
        pileService.addPileHasLotNos(pileId.value, lotNoIds, 1)

        // Record pile creation transaction
        pileService.addPileTransaction(pileId = pileId.value,
            toGoodMovementId = goodMovement.id.value,
            userId = userId.toInt(),
            type = PileTransactionDao.CREATE,
            toLotNos = lotNoIds,
            remark = "create from existing pile"
        )
    }
}