package com.champaca.inventorydata.pile.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.*
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.request.RelocatePileRequest
import com.champaca.inventorydata.pile.response.RelocatePileResponse
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class RelocatePileUseCase(
    val dataSource: DataSource,
    val pileService: PileService,
    val wmsService: WmsService
) {

    val locationRegex = "[BN][PS][A-Z]{2}[A-Z0-9]{5}".toRegex()
    val pileRegex = "[A-Z0-9]{2}\\d{4}\\d{4,6}(\\.[A-Z0-9]{2})?".toRegex()
    val palletRegex = "P[A-Z]{2}\\d{8}".toRegex()
    val logRegex = "[A-Z]{1}\\d{6,7}".toRegex()
    val countableItems = listOf(EntryType.PILE, EntryType.PALLET, EntryType.LOG)

    val logger = LoggerFactory.getLogger(RelocatePileUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, userId: String, request: RelocatePileRequest): RelocatePileResponse {
        var entries = createEntries(request.barcode)

        Database.connect(dataSource)
        transaction {
            addLogger(exposedLogger)
            entries.forEach { it.fillLotNos() }
            entries = fillStoreLocationId(entries)
        }

        val wmsBarcodes = entries.map { it.convertToWmsBarcode() }.flatten().joinToString("\n")
        if (wmsBarcodes.isEmpty()) {
            logger.warn("No valid barcode found. Barcodes: $wmsBarcodes")
            return RelocatePileResponse.Failure(errorType = PileError.WMS_VALIDATION_ERROR, errorMessage = "No valid barcode found. Barcodes: $wmsBarcodes")
        }

        logger.info("Barcode to WMS: $wmsBarcodes")
        val result = wmsService.relocation(sessionId, wmsBarcodes)
        if (result is ResultOf.Failure) {
            logger.warn("WMS validation error: ${result.message}")
            return RelocatePileResponse.Failure(errorType = PileError.WMS_VALIDATION_ERROR, errorMessage = result.message)
        }

        transaction {
            addLogger(exposedLogger)
            val now = LocalDateTime.now()
            entries.forEach { it.recordRelocation(userId.toInt(), now) }
        }
        return RelocatePileResponse.Success(count = entries.filter { countableItems.contains(it.type) }.size)
    }

    private fun createEntries(barcode: String): List<Entry> {
        return barcode.split("\n").map {
            val value = it.trim()
            when {
                value.matches(pileRegex) -> PileEntry(value, pileService)
                value.matches(locationRegex) -> LocationEntry(value)
                value.matches(palletRegex) -> PalletEntry(value, pileService)
                value.matches(logRegex) -> LogEntry(value)
                else -> UnknownEntry(value)
            }
        }
    }

    private fun fillStoreLocationId(entries: List<Entry>): List<Entry> {
        val locationCodes = entries.filter { it.type == EntryType.STORE_LOCATION }.map { it.value }
        val codeToIdMap = StoreLocationDao.find { (StoreLocation.code.inList(locationCodes)) and (StoreLocation.status eq "A") }
            .toList().associateBy({it.code}, {it.id.value})

        var currentId = -1
        for (index in entries.indices.reversed()) {
            val entry = entries[index]
            if (entry.type == EntryType.STORE_LOCATION) {
                currentId = codeToIdMap[entry.value] ?: -1
            }
            entry.toStoreLocationId = currentId
        }
        return entries
    }

    enum class EntryType {
        PILE,
        STORE_LOCATION,
        PALLET,
        LOG,
        UNKNOWN;
    }

    abstract class Entry(
        val type: EntryType,
        val value: String
    ) {
        var toStoreLocationId: Int = -1
        abstract fun fillLotNos()
        abstract fun convertToWmsBarcode(): List<String>
        open fun recordRelocation(userId: Int, now: LocalDateTime) {}
    }

    class UnknownEntry(value: String): Entry(EntryType.UNKNOWN, value) {
        override fun fillLotNos() { }
        override fun convertToWmsBarcode() = listOf<String>()
    }

    class LocationEntry(value: String): Entry(EntryType.STORE_LOCATION, value) {
        override fun fillLotNos() { }

        override fun convertToWmsBarcode(): List<String> {
            return listOf(value)
        }
    }

    class PileEntry(
            value: String,
            private val pileService: PileService
    ): Entry(EntryType.PILE, value) {
        var pileId: Int = -1
        var lotSet: Int = 1
        var pile: PileDao? = null
        var lotNos: List<LotNoDao> = listOf()
        override fun fillLotNos() {
            val pair = pileService.findPileAndCurrentLotNos(value)
            if (pair != null && pair.second.isNotEmpty()) {
                pileId = pair.first.id.value
                pile = pair.first
                lotSet = pair.first.lotSet
                lotNos = pair.second
            }
        }

        override fun convertToWmsBarcode(): List<String> {
            return if(isValid()) lotNos.map { it.code } else listOf()
        }

        override fun recordRelocation(userId: Int, now: LocalDateTime) {
            if (isValid()) {
                val entryFromStoreLocationId = pile!!.storeLocationId
                val entryToStoreLocationId = toStoreLocationId
                val entryPileId = pileId
                val entryLotSet = lotSet
                Pile.update({ Pile.id eq entryPileId }) {
                    it[this.storeLocationId] = entryToStoreLocationId
                    it[this.updatedAt] = now
                }
                PileRelocation.insert {
                    it[this.fromStoreLocationId] = entryFromStoreLocationId
                    it[this.toStoreLocationId] = entryToStoreLocationId
                    it[this.pileId] = entryPileId
                    it[this.userId] = userId
                    it[this.lotSet] = entryLotSet
                    it[this.createdAt] = now
                }
                Pile.update({Pile.id eq entryPileId}) {
                    it[this.storeLocationId] = entryToStoreLocationId
                    it[this.updatedAt] = now
                }
            }
        }

        private fun isValid(): Boolean {
            return toStoreLocationId > 0
                    && pileId > 0
                    && lotNos.isNotEmpty()
        }
    }

    class PalletEntry(
        value: String,
        private val pileService: PileService
    ): Entry(EntryType.PALLET, value) {
        val logger = LoggerFactory.getLogger(PalletEntry::class.java)

        var palletId = -1
        var pileLotNos: List<Pair<PileDao, List<LotNoDao>>> = listOf()
        override fun fillLotNos() {
            val collection = pileService.findPalletAndPilesAndCurrentLotNos(value)
            if (collection != null) {
                palletId = collection.pallet.id.value
                pileLotNos = collection.pileLotNos
            }
        }

        override fun convertToWmsBarcode(): List<String> {
            if(isValid()) {
                return pileLotNos.map { pair ->
                    pair.second.map { it.code }
                }.flatten()
            }
            return listOf()
        }

        override fun recordRelocation(userId: Int, now: LocalDateTime) {
            if (isValid()) {
                val fromStoreLocationId = pileLotNos.first().first.storeLocationId
                Pallet.update({Pallet.id eq palletId}) {
                    it[this.storeLocationId] = toStoreLocationId
                    it[this.updatedAt] = now
                }
                Pile.update({Pile.palletId eq palletId}) {
                    it[this.storeLocationId] = toStoreLocationId
                    it[this.updatedAt] = now
                }
                PileRelocation.batchInsert(pileLotNos.map { it.first }) {
                    this[PileRelocation.fromStoreLocationId] = fromStoreLocationId
                    this[PileRelocation.toStoreLocationId] = toStoreLocationId
                    this[PileRelocation.pileId] = it.id.value
                    this[PileRelocation.palletId] = palletId
                    this[PileRelocation.userId] = userId
                    this[PileRelocation.lotSet] = it.lotSet
                    this[PileRelocation.createdAt] = now
                }
            }
        }

        private fun isValid(): Boolean {
            return toStoreLocationId > 0
                    && palletId > 0
                    && pileLotNos.all { it.second.isNotEmpty() }
        }
    }

    class LogEntry(value: String): Entry(EntryType.LOG, value) {
        var previousStoreLocationId = -1
        var logLotNoId = -1
        override fun fillLotNos() {
            findLotNoIdAndStoreLocationId().let { (lotNoId, storeLocationId) ->
                if (lotNoId > 0) {
                    this.logLotNoId = lotNoId
                }
                if (storeLocationId > 0) {
                    this.previousStoreLocationId = storeLocationId
                }
            }
        }

        private fun findLotNoIdAndStoreLocationId(): Pair<Int, Int> {
            val joins = LotNo.join(StoreLocationHasLotNo, JoinType.INNER) { LotNo.id eq StoreLocationHasLotNo.lotNoId }
            val query = joins.select(LotNo.id, StoreLocationHasLotNo.storeLocationId)
                .where { LotNo.refCode eq value }
            return query.map { Pair(it[LotNo.id].value, it[StoreLocationHasLotNo.storeLocationId].value) }.firstOrNull() ?: Pair(-1, -1)
        }

        override fun convertToWmsBarcode(): List<String> {
            return listOf(value)
        }

        override fun recordRelocation(userId: Int, now: LocalDateTime) {
            if (logLotNoId > 0 && previousStoreLocationId > 0) {
                LogRelocation.insert {
                    it[LogRelocation.fromStoreLocationId] = previousStoreLocationId
                    it[LogRelocation.toStoreLocationId] = super.toStoreLocationId
                    it[LogRelocation.lotNoId] = logLotNoId
                    it[LogRelocation.userId] = userId
                    it[LogRelocation.createdAt] = now
                }
            }
        }
    }
}