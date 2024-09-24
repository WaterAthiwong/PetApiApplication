package com.champaca.inventorydata.data.report.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.data.report.response.GetKilnStatusResponse
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.skugroup.SkuGroupRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class GetKilnStatusUseCase(
    val dataSource: DataSource,
    val skuGroupRepository: SkuGroupRepository
) {

    companion object {
        val KILNS = listOf("A01", "A02", "A03", "A04", "A05", "A06", "A07", "A08", "A09", "A10", "B01", "B02", "B03", "C01", "C02", "D01", "D02", "D03",)
        val KILNS_CAPACITY = listOf(1200, 1200, 1200, 1200, 1200, 1200, 1200, 1200, 1200, 1200, 1000, 1000, 1000, 177, 177, 800, 800, 800)
        val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    val logger = LoggerFactory.getLogger(GetKilnStatusUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(): GetKilnStatusResponse {
        Database.connect(dataSource)
        var kilnStatuses = listOf<GetKilnStatusResponse.KilnStatus>()
        transaction {
            addLogger(exposedLogger)
            kilnStatuses = getKilnStatus()
        }
        return GetKilnStatusResponse(kilnStatuses)
    }

    private fun getKilnStatus(): List<GetKilnStatusResponse.KilnStatus> {
        val basicRows = getBasicInfo()
        val rowsByKiln = basicRows.groupBy { it[ManufacturingLine.name] }
        val goodMovementIds = basicRows.map { it[GoodMovement.id].value }.distinct()
        val extraAttributesByGmId = getGoodMovementExtraAttributes(goodMovementIds)
        val kilnStatuses = mutableListOf<GetKilnStatusResponse.KilnStatus>()
        val skuGroups = skuGroupRepository.getAll()
        rowsByKiln.forEach { kiln, rows ->
            val firstRow = rows.first()
            val skuGroup = skuGroups.find { it.id.value == firstRow[Sku.skuGroupId] }!!
            val description = "${skuGroup.name} + ${firstRow[Sku.species]}"
            val thicknesses = rows.map { it[Sku.thickness].setScale(2, RoundingMode.HALF_UP).toString() }.sorted().distinct().joinToString(", ")
            val maxCapacity = KILNS_CAPACITY[KILNS.indexOf(kiln)]
            val inputFt3 = rows.sumOf { it[Sku.volumnFt3].multiply(it[ManufacturingLineHasLotNo.qty]) }.setScale(2, RoundingMode.HALF_UP)
            val utilise = inputFt3.divide(maxCapacity.toBigDecimal(), 2, RoundingMode.HALF_UP).multiply(100.toBigDecimal())
            val qty = rows.sumOf { it[ManufacturingLineHasLotNo.qty] }
            val goodMovemendId = rows.first()[GoodMovement.id].value
            val extraAttributes = extraAttributesByGmId[goodMovemendId]!!
            val startDryingDate = extraAttributes["startKilnDate"]
            val expectedDate = extraAttributes["expectedDate"]
            val status = if (startDryingDate.isNullOrEmpty()) "กำลังนำไม้เข้า" else "เดินเตาแล้ว"
            val humidity = extraAttributes["humidity"]
            val orders = rows.map { it[Pile.orderNo] }.distinct().filter{ !it.isNullOrEmpty() }.joinToString(", ")
            val days = if (!expectedDate.isNullOrEmpty()) {
                Duration.between(LocalDate.now().atStartOfDay(), LocalDate.parse(expectedDate, DATE_FORMAT).atStartOfDay()).toDays().toInt()
            } else {
                null
            }
            val thicknessByRow = rows.map {
                val coeff = when (it[Sku.thicknessUom]) {
                    "mm" -> 0.00328084.toBigDecimal()
                    "cm" -> 0.0328084.toBigDecimal()
                    "inch" -> BigDecimal.ONE
                    else -> BigDecimal.ONE
                }
                it[Sku.thickness].multiply(coeff).multiply(it[ManufacturingLineHasLotNo.qty])
            }
            val averageThickness = thicknessByRow.sumOf{ it }.divide(qty, 3, RoundingMode.HALF_UP)
            kilnStatuses.add(GetKilnStatusResponse.KilnStatus(
                kiln = kiln,
                description = description,
                thicknesses = thicknesses,
                maxCapacity = maxCapacity.toBigDecimal(),
                inputFt3 = inputFt3,
                utilise = "${utilise}%",
                startDryingDate = startDryingDate,
                expectedDate = expectedDate,
                days = days,
                humidityIn = humidity,
                status = status,
                orders = orders,
                qty = qty,
                averageThickness = averageThickness,
                jobNo = rows.first()[GoodMovement.jobNo],
                goodMovementId = goodMovemendId
            ))
        }

        KILNS.forEach { kiln ->
            if (kilnStatuses.none { it.kiln == kiln }) {
                kilnStatuses.add(GetKilnStatusResponse.KilnStatus(
                    status = "ว่าง",
                    kiln = kiln
                ))
            }
        }

        return kilnStatuses.sortedBy { it.kiln }
    }

    private fun getBasicInfo(): List<ResultRow> {
        val joins = ManufacturingLineHasLotNo.join(GmItem, JoinType.INNER) { ManufacturingLineHasLotNo.lotNoId eq GmItem.lotNoId }
            .join(GoodMovement, JoinType.INNER) { (GoodMovement.id eq GmItem.goodMovementId) and (GoodMovement.type eq GoodMovementType.PICKING_ORDER.wmsName) }
            .join(Sku, JoinType.INNER) { Sku.id eq GmItem.skuId }
            .join(ManufacturingLine, JoinType.INNER) { ManufacturingLine.id eq ManufacturingLineHasLotNo.manufacturingLineId }
            .join(PileHasLotNo, JoinType.INNER) { PileHasLotNo.lotNoId eq ManufacturingLineHasLotNo.lotNoId }
            .join(Pile, JoinType.INNER) { (Pile.id eq PileHasLotNo.pileId) and (Pile.lotSet eq PileHasLotNo.lotSet) }
        val query = joins.select(ManufacturingLine.name, Sku.matCode, Sku.thickness, Sku.thicknessUom, Sku.volumnFt3,
            ManufacturingLineHasLotNo.qty, GoodMovement.id, GoodMovement.jobNo, ManufacturingLineHasLotNo.updatedAt, Sku.skuGroupId,
            Sku.species, Pile.orderNo)
            .where{ (ManufacturingLine.name inList KILNS) and (GmItem.status eq "A") }
        return query.toList()
    }

    private fun getGoodMovementExtraAttributes(goodMovementIds: List<Int>): Map<Int, Map<String, String>> {
        val query = GoodMovement.select(GoodMovement.id, GoodMovement.extraAttributes)
            .where { GoodMovement.id inList goodMovementIds }
        return query.toList().associateBy { it[GoodMovement.id].value }
            .mapValues { it.value[GoodMovement.extraAttributes] ?: emptyMap() }
    }
}