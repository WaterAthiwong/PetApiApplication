package com.champaca.inventorydata.cleansing.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.LotNo
import com.champaca.inventorydata.databasetable.Pile
import com.champaca.inventorydata.databasetable.dao.LotNoDao
import com.champaca.inventorydata.databasetable.dao.PileTransactionDao
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.config.ConfigRepository
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Deprecated("Use for import existing data only")
@Service
class PlywoodUseCase(
    val dataSource: DataSource,
    val wmsService: WmsService,
    val pileService: PileService,
    val configRepository: ConfigRepository
) {
//    val skuIds = listOf(97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 97790, 95239, 104921, 104921, 104921, 104921, 104921, 104921)
//    val matCodes = listOf("1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-10X1525X1525", "1R7RP2-8X1220X2440", "1R7BR2-12X1220X2440", "1R7BR2-12X1220X2440", "1R7BR2-12X1220X2440", "1R7BR2-12X1220X2440", "1R7BR2-12X1220X2440", "1R7BR2-12X1220X2440")
//    val goodMovementIds = listOf(4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4202, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4203, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4204, 4205, 4206, 4206, 4206, 4206, 4206, 4206)
//    val createAts = listOf("2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-04-18", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-05-25", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-06-21", "2023-08-22", "2023-12-13", "2023-12-13", "2023-12-13", "2023-12-13", "2023-12-13", "2023-12-13")
//    val qtys = listOf("40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "39", "40", "40", "40", "40", "40", "40", "40", "40", "40", "38", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "40", "39", "40", "40", "40", "40", "40", "40", "40", "96", "72", "72", "72", "72", "72", "72")
//    val pallets = listOf("PW011-23/23041802", "PW011-23/23041803", "PW011-23/23041804", "PW011-23/23041805", "PW011-23/23041806", "PW011-23/23041807", "PW011-23/23041808", "PW011-23/23041809", "PW011-23/23041810", "PW011-23/23041811", "PW011-23/23041812", "PW011-23/23041813", "PW011-23/23041814", "PW011-23/23041815", "PW011-23/23041816", "PW011-23/23041817", "PW011-23/23041818", "PW011-23/23041819", "PW011-23/23041820", "PW011-23/23041821", "PW011-23/23041822", "PW011-23/23041823", "PW011-23/23041824", "PW011-23/23041825", "PW011-23/23041826", "PW011-23/23041827", "PW011-23/23041828", "PW011-23/23041829", "PW011-23/23041830", "PW011-23/23041831", "PW011-23/23041832", "PW011-23/23041833", "PW011-23/23041835", "PW011-23/23041836", "PW011-23/23041837", "PW011-23/23041838", "TM016-PW23052501", "TM016-PW23052502", "TM016-PW23052503", "TM016-PW23052504", "TM016-PW23052505", "TM016-PW23052507", "TM016-PW23052508", "TM016-PW23052510", "TM016-PW23052511", "TM016-PW23052512", "TM016-PW23052513", "TM016-PW23052515", "TM016-PW23052516", "TM016-PW23052517", "TM016-PW23052518", "TM016-PW23052522", "TM016-PW23052523", "TM016-PW23052524", "TM016-PW23052525", "TM016-PW23052526", "TM016-PW23052527", "TM016-PW23052528", "TM016-PW23052529", "TM016-PW23052531", "TM016-PW23052532", "TM016-PW23052533", "TM016-PW23052534", "TM016-PW23052535", "TM016-PW23052536", "TM016-PW23052537", "TM016-PW23052538", "TM016-PW23052539", "TM016-PW23052540", "TM016-PW23052541", "TM020-PW23062101", "TM020-PW23062102", "TM020-PW23062103", "TM020-PW23062104", "TM020-PW23062110", "TM020-PW23062112", "TM020-PW23062113", "TM020-PW23062114", "TM020-PW23062116", "TM020-PW23062117", "TM020-PW23062118", "TM020-PW23062119", "TM020-PW23062120", "TM020-PW23062121", "TM020-PW23062124", "TM020-PW23062125", "TM020-PW23062126", "TM020-PW23062127", "TM020-PW23062128", "TM020-PW23062135", "TM020-PW23062138", "TM020-PW23062139", "TM020-PW23062140", "TM020-PW23062141", "TM051-PW23092302", "PW23121307", "PW23121308", "PW23121311", "PW23121312", "PW23121313", "PW23121316")

    val skuIds = listOf(97790)
    val matCodes = listOf("1R7RP2-10X1525X1525")
    val goodMovementIds = listOf(4202)
    val createAts = listOf("2023-04-18")
    val qtys = listOf("40")
    val pallets = listOf("PW011-23/23041801")


    val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val MONTH_FORMAT = DateTimeFormatter.ofPattern("MM")

    fun execute(sessionId: String, userId: String): List<String> {
        val pileCodes = mutableListOf<String>()
        val itemMovements = mutableListOf<WmsService.ItemMovementEntry>()

        Database.connect(dataSource)
        transaction {
            addLogger(ExposedInfoLogger)

            skuIds.forEachIndexed { index, skuId ->
                val date = LocalDateTime.parse(createAts[index] + " 08:00:00", DATE_FORMAT)
                val prefix = "WH23${date.format(MONTH_FORMAT)}"
                val config = configRepository.getOrPut("Pile.$prefix", defaultInt = 0)
                val nextRunning = (config.valueInt!! + 1).toString()
                val pileCode = "${prefix}${nextRunning.padStart(4, '0')}" // e.g. SM23110001, DK23120058
                config.valueInt = config.valueInt!! + 1

                pileCodes.add(pileCode)

                val itemMovement = WmsService.ItemMovementEntry(
                    goodMovementType = GoodMovementType.GOODS_RECEIPT,
                    goodMovementId = goodMovementIds[index],
                    skuId = skuId,
                    sku = matCodes[index],
                    storeLocationId = 574,
                    storeLocation = "BSWHZ9999",
                    manufacturingLineId = null,
                    qty = qtys[index].toBigDecimal(),
                    refCode = "${pileCode}_WH01",
                    remark = pallets[index]
                )
                itemMovements.add(itemMovement)
            }
        }

        val result = wmsService.receiveGmItem(sessionId, itemMovements)
        if (result is ResultOf.Failure) {
            println("Error: ${result.message}")
        }

        transaction {
            addLogger(ExposedInfoLogger)

            val pileToLotNoMap = getLotNoFromRefCode(itemMovements.map { it.refCode!! })
            upsertPileRecords(userId, itemMovements, pileCodes, pileToLotNoMap)
        }
        return pileCodes
    }

    private fun getLotNoFromRefCode(refCodes: List<String>): Map<String, LotNoDao> {
        return LotNoDao.find { (LotNo.refCode.inList(refCodes)) and (LotNo.status eq "A") }.toList().associateBy({it.refCode.substringBefore('_')}, {it})
    }

    private fun upsertPileRecords(userId: String, itemMovements: List<WmsService.ItemMovementEntry>, pileCodes: List<String>, pileToLotNoMap: Map<String, LotNoDao>) {
        itemMovements.forEachIndexed { index, movement ->
            val dateTime = LocalDateTime.parse(createAts[index] + " 08:00:00", DATE_FORMAT)

            val pileId = Pile.insertAndGetId {
                it[this.goodMovementId] = goodMovementIds[index]
                it[this.originGoodMovementId] = goodMovementIds[index]
                it[this.code] = pileCodes[index]
                it[this.processTypePrefix] = "WH"
                it[this.lotSet] = 1
                it[this.type] = "woodPile"
                it[this.remark] = movement.remark
                it[this.createdAt] = dateTime
                it[this.updatedAt] = dateTime
                it[this.status] = "A"
            }

            val lotNoId = pileToLotNoMap[pileCodes[index]]!!.id.value
            pileService.addPileHasLotNos(pileId.value, listOf(lotNoId), 1)

            pileService.addPileTransaction(pileId = pileId.value,
                toGoodMovementId = goodMovementIds[index],
                userId = userId.toInt(),
                type = PileTransactionDao.CREATE,
                toLotNos = listOf(lotNoId),
                remainingQty = listOf(qtys[index].toBigDecimal()),
                dateTime = dateTime
            )
        }
    }
}