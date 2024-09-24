package com.champaca.inventorydata.pile

import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.*
import com.champaca.inventorydata.databasetable.dao.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.goodmovement.usecase.GetPileDetailsUseCase
import com.champaca.inventorydata.masterdata.config.ConfigRepository
import com.champaca.inventorydata.masterdata.sku.SkuRepository
import com.champaca.inventorydata.masterdata.user.UserRepository
import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.model.PileEntry
import com.champaca.inventorydata.utils.DateTimeUtil
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class PileService(
    val skuRepository: SkuRepository,
    val userRepository: UserRepository,
    val configRepository: ConfigRepository,
    val dateTimeUtil: DateTimeUtil
) {

    fun newPileCodeAndIncreaseRunningNumber(prefix: String): ResultOf<String> {
        val result = newPileCodeAndIncreaseRunningNumber(prefix, 1)
        return when (result) {
            is ResultOf.Success -> ResultOf.Success(result.value.first())
            is ResultOf.Failure -> ResultOf.Failure(result.message)
        }
    }

    fun newPileCodeAndIncreaseRunningNumber(prefix: String, copies: Int, padLength: Int = 4): ResultOf<List<String>> {
        val monthPrefix = dateTimeUtil.getYearMonthPrefix()
        val pilePrefix = "${prefix}${monthPrefix}" // e.g. CX2403
        val config = configRepository.getOrPut("Pile.${pilePrefix}", defaultInt = 0)
        val nextRunning = config.valueInt!! + 1
        val newPileCodes = List(copies) {
            "${pilePrefix}${(nextRunning + it).toString().padStart(padLength, '0')}" // e.g. CX24030001, CX24030058
        }

        val pile = PileDao.find { (Pile.code inList newPileCodes) and (Pile.status eq "A") }.firstOrNull()
        if (pile != null) {
            // ที่ต้องใส่ตรงนี้มาดักไว้ตรงนี้เพราะว่าเคยเจอเคสที่เลข running จาก cpc_config มันจู่ๆก็ย้อนกลับไปสิบกว่าเลข
            // ทำให้สร้างกองไม้ที่มีเลขซ้ำ มั่วไปยาวๆเลย กว่าจะแก้เสร็จเสียเวลามาก
            return ResultOf.Failure(pile.code)
        }

        config.valueInt = config.valueInt!! + copies

        return ResultOf.Success(newPileCodes)
    }

    fun findPileAndCurrentLotNos(pileCode: String, needToBeActive: Boolean = true): Pair<PileDao, List<LotNoDao>>? {
        val pileQuery = Pile.select(Pile.columns).where { Pile.code eq pileCode }
        if (needToBeActive) {
            pileQuery.andWhere { Pile.status eq "A" }
        }
        val pile = PileDao.wrapRow(pileQuery.singleOrNull() ?: return null)

        val query = LotNo.join(PileHasLotNo, JoinType.INNER) { LotNo.id eq PileHasLotNo.lotNoId }
            .select(LotNo.columns)
            .where { (PileHasLotNo.pileId eq pile.id.value) and (PileHasLotNo.lotSet eq pile.lotSet) and (LotNo.status eq "A") }
        val lotNos = LotNoDao.wrapRows(query).toList()
        return Pair(pile, lotNos)
    }

    fun findPalletAndPilesAndCurrentLotNos(palletCode: String): PalletCollection? {
        val pallet = PalletDao.find { (Pallet.code eq palletCode) and (Pallet.status eq "A") }.singleOrNull() ?: return null

        val piles = pallet.piles.toList()
        if (piles.isEmpty()) return PalletCollection(pallet, listOf())

        val compoundCondition = piles.fold(null as Op<Boolean>?) { acc, pile ->
            if (acc == null) {
                (PileHasLotNo.pileId eq pile.id.value) and (PileHasLotNo.lotSet eq pile.lotSet)
            } else {
                acc or ((PileHasLotNo.pileId eq pile.id.value) and (PileHasLotNo.lotSet eq pile.lotSet))
            }
        }

        val query = PileHasLotNo.join(LotNo, JoinType.INNER) { PileHasLotNo.lotNoId eq LotNo.id }
            .select(listOf(PileHasLotNo.pileId) + LotNo.columns)
            .where { compoundCondition!! and (LotNo.status eq "A") }
        val lotNoMap = LotNoDao.wrapRows(query).toList().associateBy({it.id.value}, {it})
        val pileIdToLotNoIdsMap = query.map { resultRow -> Pair(resultRow[PileHasLotNo.pileId].value, resultRow[LotNo.id].value) }
            .toList()
            .groupBy({it.first}, {it.second})

        val pileLotNos = piles.map {  pile ->
            val lotNos = pileIdToLotNoIdsMap[pile.id.value]!!.map { lotNoMap[it]!! }
            Pair(pile, lotNos)
        }

        return PalletCollection(pallet, pileLotNos)
    }

    fun findPileListAndCurrentLotNos(piles: List<PileDao>): List<Pair<PileDao, List<LotNoDao>>> {
        val pileIds = piles.map { it.id.value }
        val query = PileHasLotNo.join(LotNo, JoinType.INNER) { PileHasLotNo.lotNoId eq LotNo.id }
            .select(PileHasLotNo.pileId, PileHasLotNo.lotSet, LotNo.id)
            .where { (PileHasLotNo.pileId inList pileIds) and (LotNo.status eq "A") }

        val queryResults = query.map { resultRow ->
            Triple(resultRow[PileHasLotNo.pileId].value, resultRow[PileHasLotNo.lotSet], resultRow[LotNo.id].value)
        }.groupBy({it.first}, { Pair(it.second, it.third) })

        val pileIdToLotIdsMap = queryResults.mapValues { entry ->
            val maxLotSet = entry.value.maxBy { it.first }.first
            entry.value.filter { it.first == maxLotSet }.map { it.second }
        }

        val lotNoIdToDaoMap = LotNoDao.find { LotNo.status eq "A" and (LotNo.id inList pileIdToLotIdsMap.values.flatten()) }.toList()
            .associateBy({it.id.value}, {it})
        val results = mutableListOf<Pair<PileDao, List<LotNoDao>>>()
        for (pile in piles) {
            val lotNoIds = pileIdToLotIdsMap[pile.id.value]!!
            val lotNos = lotNoIds.map { lotNoIdToDaoMap[it]!! }
            results.add(Pair(pile, lotNos))
        }
        return results
    }

    fun findItemsInStorageArea(lotNoIds: List<Int>): List<MovingItem> {
        // Items to be picked need to be in storage area i.e. they are present in StoreLocationHasLotNo.
        val joins = StoreLocation.join(StoreLocationHasLotNo, JoinType.INNER) { StoreLocation.id eq StoreLocationHasLotNo.storeLocationId }
            .join(LotNo, JoinType.INNER) { LotNo.id eq StoreLocationHasLotNo.lotNoId }
            .join(Sku, JoinType.INNER) { Sku.id eq StoreLocationHasLotNo.skuId }
            .join(SkuGroup, JoinType.INNER) { SkuGroup.id eq Sku.skuGroupId }

        val query = joins.select(LotNo.id, LotNo.code, LotNo.refCode, Sku.id, Sku.matCode, Sku.name, Sku.width, Sku.width,
            Sku.widthUom, Sku.length, Sku.lengthUom, Sku.thickness, Sku.thicknessUom, Sku.volumnFt3, Sku.volumnM3,
            Sku.grade, Sku.fsc, Sku.species, Sku.skuGroupId,StoreLocation.id, StoreLocation.code,
            StoreLocationHasLotNo.qty, SkuGroup.erpMainGroupName)
            .where { (LotNo.status eq "A") and (Sku.status eq "A") and LotNo.id.inList(lotNoIds) }
            .orderBy(LotNo.createdAt to SortOrder.ASC)

        return query.map { resultRow ->
            MovingItem(
                lotNoId = resultRow[LotNo.id].value,
                lotCode = resultRow[LotNo.code],
                lotRefCode = resultRow[LotNo.refCode],
                skuId = resultRow[Sku.id].value,
                matCode = resultRow[Sku.matCode],
                skuName = resultRow[Sku.name],
                width = resultRow[Sku.width],
                widthUom = resultRow[Sku.widthUom],
                length = resultRow[Sku.length],
                lengthUom = resultRow[Sku.lengthUom],
                thickness = resultRow[Sku.thickness],
                thicknessUom = resultRow[Sku.thicknessUom],
                volumnFt3 = resultRow[Sku.volumnFt3],
                volumnM3 = resultRow[Sku.volumnM3],
                grade = resultRow[Sku.grade],
                fsc = resultRow[Sku.fsc],
                species = resultRow[Sku.species],
                skuGroupId = resultRow[Sku.skuGroupId],
                storeLocationId = resultRow[StoreLocation.id].value,
                storeLocationCode = resultRow[StoreLocation.code],
                qty = resultRow[StoreLocationHasLotNo.qty]
            ).apply{
                skuMainGroupName = resultRow[SkuGroup.erpMainGroupName]
            }
        }
    }

    fun findItemsInStorageArea(pileCodes: List<String>, skuIds: List<Int>): List<MovingItem> {
        // Items to be picked need to be in storage area i.e. they are present in StoreLocationHasLotNo.
        val joins = StoreLocation.join(StoreLocationHasLotNo, JoinType.INNER) { StoreLocation.id eq StoreLocationHasLotNo.storeLocationId }
            .join(LotNo, JoinType.INNER) { LotNo.id eq StoreLocationHasLotNo.lotNoId }
            .join(Sku, JoinType.INNER) { Sku.id eq StoreLocationHasLotNo.skuId }
            .join(SkuGroup, JoinType.INNER) { SkuGroup.id eq Sku.skuGroupId }
            .join(PileHasLotNo, JoinType.INNER) { PileHasLotNo.lotNoId eq LotNo.id }
            .join(Pile, JoinType.INNER) { (Pile.id eq PileHasLotNo.pileId) and (Pile.lotSet eq PileHasLotNo.lotSet) }

        val query = joins.select(LotNo.id, LotNo.code, LotNo.refCode, Sku.id, Sku.matCode, Sku.name, Sku.width, Sku.width,
            Sku.widthUom, Sku.length, Sku.lengthUom, Sku.thickness, Sku.thicknessUom, Sku.volumnFt3, Sku.volumnM3,
            Sku.grade, Sku.fsc, Sku.species, Sku.skuGroupId,StoreLocation.id, StoreLocation.code,
            StoreLocationHasLotNo.qty, SkuGroup.erpMainGroupName, Pile.code)
            .where { (LotNo.status eq "A") and (Sku.status eq "A") and (Sku.id inList skuIds) and (Pile.code inList pileCodes) }
            .orderBy(LotNo.createdAt to SortOrder.ASC)

        return query.map { resultRow ->
            MovingItem(
                lotNoId = resultRow[LotNo.id].value,
                lotCode = resultRow[LotNo.code],
                lotRefCode = resultRow[LotNo.refCode],
                skuId = resultRow[Sku.id].value,
                matCode = resultRow[Sku.matCode],
                skuName = resultRow[Sku.name],
                width = resultRow[Sku.width],
                widthUom = resultRow[Sku.widthUom],
                length = resultRow[Sku.length],
                lengthUom = resultRow[Sku.lengthUom],
                thickness = resultRow[Sku.thickness],
                thicknessUom = resultRow[Sku.thicknessUom],
                volumnFt3 = resultRow[Sku.volumnFt3],
                volumnM3 = resultRow[Sku.volumnM3],
                grade = resultRow[Sku.grade],
                fsc = resultRow[Sku.fsc],
                species = resultRow[Sku.species],
                skuGroupId = resultRow[Sku.skuGroupId],
                storeLocationId = resultRow[StoreLocation.id].value,
                storeLocationCode = resultRow[StoreLocation.code],
                qty = resultRow[StoreLocationHasLotNo.qty],
            ).apply{
                skuMainGroupName = resultRow[SkuGroup.erpMainGroupName]
                pilecode = resultRow[Pile.code]
            }
        }
    }

    fun findReceivingItems(lotNoIds: List<Int>, pickingGoodMovementIds: List<Int>): List<MovingItem> {
        // Item to be received may no longer present on both storage area and manufacturing line because when a user link
        // "Goods Receipt" goods movement with "Picking Order", items on "Picking Order" will be removed from ManufacturingLineHasLotNo
        // Hence we can't find them using StoreLocationHasLotNo and ManufacturingLineHasLotNo but rely just on GmItem
        val joins = LotNo.join(GmItem, JoinType.INNER) { LotNo.id eq GmItem.lotNoId }
            .join(Sku, JoinType.INNER) { Sku.id eq GmItem.skuId }
            .join(GoodMovement, JoinType.INNER) { GoodMovement.id eq GmItem.goodMovementId }

        val query = joins.select(LotNo.id, LotNo.code, LotNo.refCode,
            Sku.id, Sku.matCode, Sku.name, Sku.width, Sku.width, Sku.widthUom, Sku.length, Sku.lengthUom, Sku.thickness,
            Sku.thicknessUom, Sku.volumnFt3, Sku.volumnM3, Sku.grade, Sku.fsc, Sku.species, Sku.skuGroupId,
            GmItem.qty, GoodMovement.id)
            .where { (LotNo.status eq "A") and (Sku.status eq "A") and (GoodMovement.type eq GoodMovementType.PICKING_ORDER.wmsName) and
                    (GmItem.status eq "A") and (GoodMovement.id.inList(pickingGoodMovementIds)) and LotNo.id.inList(lotNoIds)}

        return query.map { resultRow ->
            MovingItem(
                lotNoId = resultRow[LotNo.id].value,
                lotCode = resultRow[LotNo.code],
                lotRefCode = resultRow[LotNo.refCode],
                skuId = resultRow[Sku.id].value,
                matCode = resultRow[Sku.matCode],
                skuName = resultRow[Sku.name],
                width = resultRow[Sku.width],
                widthUom = resultRow[Sku.widthUom],
                length = resultRow[Sku.length],
                lengthUom = resultRow[Sku.lengthUom],
                thickness = resultRow[Sku.thickness],
                thicknessUom = resultRow[Sku.thicknessUom],
                volumnFt3 = resultRow[Sku.volumnFt3],
                volumnM3 = resultRow[Sku.volumnM3],
                grade = resultRow[Sku.grade],
                fsc = resultRow[Sku.fsc],
                species = resultRow[Sku.species],
                skuGroupId = resultRow[Sku.skuGroupId],
                storeLocationId = -1,
                storeLocationCode = "",
                qty = resultRow[GmItem.qty]
            )
        }
    }

    fun findItemsInProcess(lotNoIds: List<Int>, manufacturingLineId: Int): List<MovingItem> {
        val joins = ManufacturingLine.join(ManufacturingLineHasLotNo, JoinType.INNER) { ManufacturingLine.id eq ManufacturingLineHasLotNo.manufacturingLineId }
            .join(LotNo, JoinType.INNER) { LotNo.id eq ManufacturingLineHasLotNo.lotNoId }
            .join(Sku, JoinType.INNER) { Sku.id eq ManufacturingLineHasLotNo.skuId }

        val query = joins.select(LotNo.id, LotNo.code, LotNo.refCode, Sku.id, Sku.matCode, Sku.name, Sku.width,
            Sku.width, Sku.widthUom, Sku.length, Sku.lengthUom, Sku.thickness, Sku.thicknessUom, Sku.volumnFt3,
            Sku.volumnM3, Sku.grade, Sku.fsc, Sku.species, Sku.skuGroupId, ManufacturingLineHasLotNo.qty)
            .where { (LotNo.status eq "A") and (Sku.status eq "A") and (LotNo.id.inList(lotNoIds)) and
                    (ManufacturingLine.id eq manufacturingLineId)}

        return query.map { resultRow ->
            MovingItem(
                lotNoId = resultRow[LotNo.id].value,
                lotCode = resultRow[LotNo.code],
                lotRefCode = resultRow[LotNo.refCode],
                skuId = resultRow[Sku.id].value,
                matCode = resultRow[Sku.matCode],
                skuName = resultRow[Sku.name],
                width = resultRow[Sku.width],
                widthUom = resultRow[Sku.widthUom],
                length = resultRow[Sku.length],
                lengthUom = resultRow[Sku.lengthUom],
                thickness = resultRow[Sku.thickness],
                thicknessUom = resultRow[Sku.thicknessUom],
                volumnFt3 = resultRow[Sku.volumnFt3],
                volumnM3 = resultRow[Sku.volumnM3],
                grade = resultRow[Sku.grade],
                fsc = resultRow[Sku.fsc],
                species = resultRow[Sku.species],
                skuGroupId = resultRow[Sku.skuGroupId],
                storeLocationId = -1,
                storeLocationCode = "",
                qty = resultRow[ManufacturingLineHasLotNo.qty]
            )
        }
    }

    fun findPileInitialItems(pile: PileDao): List<MovingItem> {
        val joins = PileHasLotNo.join(GmItem, JoinType.INNER) { PileHasLotNo.lotNoId eq GmItem.lotNoId }
            .join(LotNo, JoinType.INNER) { LotNo.id eq GmItem.lotNoId }
            .join(Sku, JoinType.INNER) { GmItem.skuId eq Sku.id }
        val query = joins.select(LotNo.id, LotNo.code, LotNo.refCode, Sku.id, Sku.matCode, Sku.name, Sku.width,
            Sku.width, Sku.widthUom, Sku.length, Sku.lengthUom, Sku.thickness, Sku.thicknessUom, Sku.volumnFt3,
            Sku.volumnM3, Sku.grade, Sku.fsc, Sku.species, Sku.skuGroupId, GmItem.qty)
            .where { (GmItem.goodMovementId eq pile.originGoodMovementId) and (PileHasLotNo.pileId eq pile.id.value) and
                    (PileHasLotNo.lotSet eq 1) and (LotNo.status eq "A") and (GmItem.status eq "A")}
        return query.map { resultRow ->
            MovingItem(
                lotNoId = resultRow[LotNo.id].value,
                lotCode = resultRow[LotNo.code],
                lotRefCode = resultRow[LotNo.refCode],
                skuId = resultRow[Sku.id].value,
                matCode = resultRow[Sku.matCode],
                skuName = resultRow[Sku.name],
                width = resultRow[Sku.width],
                widthUom = resultRow[Sku.widthUom],
                length = resultRow[Sku.length],
                lengthUom = resultRow[Sku.lengthUom],
                thickness = resultRow[Sku.thickness],
                thicknessUom = resultRow[Sku.thicknessUom],
                volumnFt3 = resultRow[Sku.volumnFt3],
                volumnM3 = resultRow[Sku.volumnM3],
                grade = resultRow[Sku.grade],
                fsc = resultRow[Sku.fsc],
                species = resultRow[Sku.species],
                skuGroupId = resultRow[Sku.skuGroupId],
                storeLocationId = -1,
                storeLocationCode = "",
                qty = resultRow[GmItem.qty]
            )
        }
    }
    fun findPileInitialItemsInCodes(pilecodes: List<String>): List<MovingItem> {

        val piles: SizedIterable<PileDao> = PileDao.find { (Pile.code inList pilecodes ) and (Pile.status eq "A") }
        val pileOriginGoodMovementIds = piles.map { it.originGoodMovementId }

        val joins = PileHasLotNo.join(GmItem, JoinType.INNER) { PileHasLotNo.lotNoId eq GmItem.lotNoId }
            .join(LotNo, JoinType.INNER) { LotNo.id eq GmItem.lotNoId }
            .join(Sku, JoinType.INNER) { GmItem.skuId eq Sku.id }
            .join(SkuGroup, JoinType.INNER) { Sku.skuGroupId eq SkuGroup.id }
            .join(Pile, JoinType.INNER) { Pile.id eq PileHasLotNo.pileId }

        val query = joins.select(Pile.code,LotNo.id, LotNo.code, LotNo.refCode, Sku.id, Sku.matCode, Sku.name, Sku.width,
            Sku.width, Sku.widthUom, Sku.length, Sku.lengthUom, Sku.thickness, Sku.thicknessUom, Sku.volumnFt3,
            Sku.volumnM3, Sku.areaM2, Sku.grade, Sku.fsc, Sku.species, Sku.skuGroupId, GmItem.qty ,SkuGroup.erpGroupCode,
            SkuGroup.name)
            .where { (GmItem.goodMovementId inList pileOriginGoodMovementIds) and
                    (PileHasLotNo.lotSet eq 1) and (LotNo.status eq "A") and (GmItem.status eq "A")}

        return query.map { resultRow ->
            MovingItem(
                lotNoId = resultRow[LotNo.id].value,
                lotCode = resultRow[LotNo.code],
                lotRefCode = resultRow[LotNo.refCode],
                skuId = resultRow[Sku.id].value,
                matCode = resultRow[Sku.matCode],
                skuName = resultRow[Sku.name],
                width = resultRow[Sku.width],
                widthUom = resultRow[Sku.widthUom],
                length = resultRow[Sku.length],
                lengthUom = resultRow[Sku.lengthUom],
                thickness = resultRow[Sku.thickness],
                thicknessUom = resultRow[Sku.thicknessUom],
                volumnFt3 = resultRow[Sku.volumnFt3],
                volumnM3 = resultRow[Sku.volumnM3],
                areaM2 = resultRow[Sku.areaM2] ?: BigDecimal.ZERO,
                grade = resultRow[Sku.grade],
                fsc = resultRow[Sku.fsc],
                species = resultRow[Sku.species],
                skuGroupId = resultRow[Sku.skuGroupId],
                storeLocationId = -1,
                storeLocationCode = "",
                qty = resultRow[GmItem.qty]
            ).apply{
                pilecode = resultRow[Pile.code]
                erpGroupCode = resultRow[SkuGroup.erpGroupCode]
                skuGroupName = resultRow[SkuGroup.name]
                matCode = resultRow[Sku.matCode]
            }
        }
    }
    fun findShelfItemsInCodes(pilecodes: List<String>): List<MovingItem> {

        val joins = StoreLocationHasLotNo.join(LotNo, JoinType.LEFT) { LotNo.id eq StoreLocationHasLotNo.lotNoId }
            .join(Sku, JoinType.INNER) { Sku.id eq StoreLocationHasLotNo.skuId }
            .join(SkuGroup, JoinType.INNER) { SkuGroup.id eq Sku.skuGroupId }
            .join(PileHasLotNo, JoinType.INNER) { PileHasLotNo.lotNoId eq LotNo.id }
            .join(Pile, JoinType.INNER) { Pile.id eq PileHasLotNo.pileId }

        val query = joins.select(Pile.code,LotNo.id, LotNo.code, LotNo.refCode, Sku.id, Sku.matCode, Sku.name, Sku.width,
            Sku.width, Sku.widthUom, Sku.length, Sku.lengthUom, Sku.thickness, Sku.thicknessUom, Sku.volumnFt3,
            Sku.volumnM3, Sku.areaM2, Sku.grade, Sku.fsc, Sku.species, Sku.skuGroupId, StoreLocationHasLotNo.qty ,SkuGroup.erpGroupCode,
            SkuGroup.name)
            .where { (LotNo.status eq "A") and (Sku.status eq "A") and Pile.code.inList(pilecodes) }
            .orderBy(LotNo.createdAt to SortOrder.ASC)

        return query.map { resultRow ->
            MovingItem(
                lotNoId = resultRow[LotNo.id].value,
                lotCode = resultRow[LotNo.code],
                lotRefCode = resultRow[LotNo.refCode],
                skuId = resultRow[Sku.id].value,
                matCode = resultRow[Sku.matCode],
                skuName = resultRow[Sku.name],
                width = resultRow[Sku.width],
                widthUom = resultRow[Sku.widthUom],
                length = resultRow[Sku.length],
                lengthUom = resultRow[Sku.lengthUom],
                thickness = resultRow[Sku.thickness],
                thicknessUom = resultRow[Sku.thicknessUom],
                volumnFt3 = resultRow[Sku.volumnFt3],
                volumnM3 = resultRow[Sku.volumnM3],
                areaM2 = resultRow[Sku.areaM2] ?: BigDecimal.ZERO,
                grade = resultRow[Sku.grade],
                fsc = resultRow[Sku.fsc],
                species = resultRow[Sku.species],
                skuGroupId = resultRow[Sku.skuGroupId],
                storeLocationId = -1,
                storeLocationCode = "",
                qty = resultRow[StoreLocationHasLotNo.qty]
            ).apply{
                pilecode = resultRow[Pile.code]
                erpGroupCode = resultRow[SkuGroup.erpGroupCode]
                skuGroupName = resultRow[SkuGroup.name]
                matCode = resultRow[Sku.matCode]
            }
        }
    }

    fun addPileTransaction(pileId: Int,
                           fromGoodMovementId: Int? = null,
                           toGoodMovementId: Int? = null,
                           userId: Int,
                           type: String,
                           fromPiles: List<String> = listOf(),
                           fromLotNos: List<Int> = listOf(),
                           toPileId: Int? = null,
                           toLotNos: List<Int> = listOf(),
                           movingQty: List<BigDecimal> = listOf(),
                           remainingQty: List<BigDecimal> = listOf(),
                           palletId: Int? = null,
                           remark: String? = null,
                           dateTime: LocalDateTime = LocalDateTime.now()
    ) {
        val movingQtyStr = if (movingQty.isNotEmpty()) {
            movingQty.map { it.setScale(2, RoundingMode.HALF_UP) }
                .joinToString(",")
                .replace(".00", "")
                .take(64)
        } else null
        val remainingQtyStr = if (remainingQty.isNotEmpty()) {
            remainingQty.map { it.setScale(2, RoundingMode.HALF_UP) }
                .joinToString(",")
                .replace(".00", "")
                .take(64)
        } else null
        PileTransaction.insert {
            it[this.pileId] = pileId
            it[this.fromGoodMovementId] = fromGoodMovementId
            it[this.toGoodMovementId] = toGoodMovementId
            it[this.palletId] = palletId
            it[this.userId] = userId
            it[this.type] = type
            it[this.fromPile] = if (fromPiles.isNotEmpty()) fromPiles.joinToString(",").take(256) else null
            it[this.toPileId] = toPileId
            it[this.fromLotNos] = if (fromLotNos.isNotEmpty()) fromLotNos.joinToString(",").take(256) else null
            it[this.toLotNos] = if (toLotNos.isNotEmpty()) toLotNos.joinToString(",").take(256) else null
            it[this.movingQty] = movingQtyStr
            it[this.remainingQty] = remainingQtyStr
            it[this.remark] = remark?.take(45)
            it[this.createdAt] = dateTime
        }
    }

    fun addPileHasLotNos(pileId: Int, lotNoIds: List<Int>, lotSet: Int) {
        PileHasLotNo.batchInsert(lotNoIds) {
            this[PileHasLotNo.lotNoId] = it
            this[PileHasLotNo.pileId] = pileId
            this[PileHasLotNo.lotSet] = lotSet
            this[PileHasLotNo.createdAt] = LocalDateTime.now()
        }
    }

    data class TransferredLots(
        val lotNoId: Int,
        val transferredGoodMovementId: Int,
        val transferredLotNoId: Int,
        val transferredQty: BigDecimal
    )

    fun createNewReceivingItems(old: List<MovingItem>, newSkuGroupCode: String): ResultOf<List<MovingItem>> {
        val newMatCodes = old.map { createNewMatcode(newSkuGroupCode, it.matCode) }.toSet().toList()
        val nonExistingMatCodes = skuRepository.findNonExistingMatCodes(newMatCodes)
        if (nonExistingMatCodes.isNotEmpty()) {
            return ResultOf.Failure("Non existing matcodes: ${nonExistingMatCodes.joinToString(", ")}")
        }

        val skuMap = newMatCodes.map { skuRepository.findByMatCode(it)!! }.associateBy({it.matCode.substring(3)}, {it})
        val new = old.map {
            val sku = skuMap[it.matCode.substring(3)]!!
            createNewMovingItem(it, sku)
        }
        return ResultOf.Success(new)
    }

    fun findPileByDateLineId(userId: Int, productionDateFrom: LocalDate?, productionDateTo: LocalDate?,
                             manufacturingLineId: Int?, supplierId: Int?, pileCodes: List<String>? , departmentId: Int? ,
                             createdDateFrom: LocalDate? ,createdDateTo: LocalDate? ,type:List<String>?): List<PileEntry> {

        val joins = Pile.join(GoodMovement, JoinType.LEFT) { Pile.originGoodMovementId eq GoodMovement.id }
            .join(ManufacturingLine, JoinType.LEFT) { ManufacturingLine.id eq  GoodMovement.manufacturingLineId}
            .join(Supplier, JoinType.LEFT) { Supplier.id  eq GoodMovement.supplierId}
            .join(PileTransaction, JoinType.INNER) { (PileTransaction.pileId  eq Pile.id) and (PileTransaction.type inList listOf("assemble", "create"))}
            .join(User, JoinType.INNER) { User.id  eq PileTransaction.userId }
            .join(StoreLocation, JoinType.LEFT) { StoreLocation.id  eq Pile.storeLocationId }
            .join(StoreZone, JoinType.LEFT) { StoreZone.id  eq StoreLocation.storeZoneId }

        val query = joins.select(Pile.id,Pile.code,Pile.goodMovementId, Pile.originGoodMovementId, Pile.processTypePrefix,Pile.printedAt,
            GoodMovement.manufacturingLineId,GoodMovement.productionDate,Pile.remark,
            GoodMovement.orderNo, GoodMovement.jobNo, GoodMovement.poNo, GoodMovement.invoiceNo, GoodMovement.lotNo,
            GoodMovement.departmentId, StoreZone.departmentId, StoreLocation.name, ManufacturingLine.name, Supplier.name,Pile.createdAt,
            User.firstname, User.lastname, GoodMovement.id,Pile.extraAttributes,GoodMovement.extraAttributes)
            .where { (Pile.status eq "A") and (GoodMovement.status eq "A") }

        if (pileCodes != null&&pileCodes.size>0) {
            query.andWhere { Pile.code.inList(pileCodes) }
        }
        else {
            if (productionDateFrom != null) {
                query.andWhere { (GoodMovement.productionDate greaterEq productionDateFrom) }
            }
            if (productionDateTo != null) {
                query.andWhere { (GoodMovement.productionDate lessEq productionDateTo) }
            }
            if (manufacturingLineId != null) {
                query.andWhere { (GoodMovement.manufacturingLineId eq manufacturingLineId) }
            }
            if (supplierId != null) {
                query.andWhere { (GoodMovement.supplierId eq supplierId) }
            }
            if (departmentId != null) {
                query.andWhere { (GoodMovement.departmentId eq departmentId) }
            }
            if (createdDateFrom != null) {
                query.andWhere { (Pile.createdAt greaterEq createdDateFrom.atTime(0, 0, 0)) }
            }
            if (createdDateTo != null) {
                query.andWhere { (Pile.createdAt lessEq createdDateTo.atTime(23, 59, 59)) }
            }
            if (type != null&&type.size>0) {
                query.andWhere { (Pile.type.inList(type)) }
            }
        }

        return query.map { resultRow ->
            val actionable = checkPileActionable(userId, resultRow[Pile.goodMovementId].value, resultRow)
            PileEntry(
                id = resultRow[Pile.id].value,
                code = resultRow[Pile.code],
                goodMovementId = resultRow[Pile.goodMovementId].value,
                originGoodMovementId = resultRow[Pile.originGoodMovementId],
                originDepartmentId = resultRow[GoodMovement.departmentId].value,
                currentDepartmentId = resultRow[StoreZone.departmentId] as? Int,
                currentStoreLocation = resultRow[StoreLocation.name],
                processTypePrefix = resultRow[Pile.processTypePrefix],
                manufacturingLineId = resultRow[GoodMovement.manufacturingLineId],
                productionDate = resultRow[GoodMovement.productionDate],
                orderNo = resultRow[GoodMovement.orderNo],
                jobNo = resultRow[GoodMovement.jobNo],
                currentjobNo = "",
                poNo = resultRow[GoodMovement.poNo],
                invoiceNo = resultRow[GoodMovement.invoiceNo],
                lotNo = resultRow[GoodMovement.lotNo],
                manufacturingLinename = resultRow[ManufacturingLine.name],
                suppliername = resultRow[Supplier.name],
                remark = resultRow[Pile.remark],
                sku = null,
                type = null,
                fsc = null,
                creator = resultRow[User.firstname]+" "+resultRow[User.lastname],
                printedAt = resultRow[Pile.printedAt],
                details = null,
                lots = null,
                canEdit = actionable.canEdit,
                canRemove = actionable.canRemove,
                canUndo = actionable.canUndo,
                preferredMeasurement = null,
                createdAt = resultRow[Pile.createdAt]?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                matCode = null,
                grade = null,
                customer = resultRow[Pile.extraAttributes]?.get("customer") ?: " ",
                countryOfOrigin = resultRow[GoodMovement.extraAttributes]?.get("countryOfOrigin") ?: " ",
            )
        }
    }

    fun checkPileActionable(userId: Int, currentGoodMovementId: Int, row: ResultRow): PileActionable {
        val permissions = userRepository.getUserPermissions(userId)
        val canEdit = permissions.contains(GetPileDetailsUseCase.PILE_EDIT_PERMISSION) && row[Pile.goodMovementId].value == row[Pile.originGoodMovementId] &&
                row[StoreZone.departmentId] == row[GoodMovement.departmentId].value
        val canRemove = permissions.contains(GetPileDetailsUseCase.PILE_REMOVE_PERMISSION) && row[Pile.goodMovementId].value == row[Pile.originGoodMovementId] &&
                row[StoreZone.departmentId] == row[GoodMovement.departmentId].value
        val canUndo = permissions.contains(GetPileDetailsUseCase.PILE_UNDO_PERMISSION) && row[Pile.goodMovementId].value == currentGoodMovementId
        return PileActionable(canEdit, canRemove, canUndo)
    }

    fun findPileByListCode(userId: Int, piles: List<String>): List<PileEntry> {
        val joins = Pile.join(GoodMovement, JoinType.LEFT) { Pile.goodMovementId eq GoodMovement.id }
            .join(ManufacturingLine, JoinType.LEFT) { ManufacturingLine.id eq GoodMovement.manufacturingLineId}
            .join(Supplier, JoinType.LEFT) { GoodMovement.supplierId eq Supplier.id }
            .join(PileTransaction, JoinType.INNER) { (PileTransaction.pileId  eq Pile.id) and (PileTransaction.type inList listOf("assemble", "create"))}
            .join(User, JoinType.INNER) { User.id  eq PileTransaction.userId }

        val query = joins.select(
            Pile.id, Pile.code, Pile.goodMovementId, Pile.originGoodMovementId, Pile.processTypePrefix,
            GoodMovement.manufacturingLineId, GoodMovement.productionDate, Pile.remark,Pile.printedAt,
            Pile.orderNo, GoodMovement.jobNo, GoodMovement.poNo, GoodMovement.invoiceNo, GoodMovement.lotNo,
            ManufacturingLine.name, Supplier.name, User.firstname,User.lastname, GoodMovement.departmentId,
            Pile.createdAt,Pile.extraAttributes, GoodMovement.extraAttributes
        )
            .where { (Pile.status eq "A") and (GoodMovement.status eq "A") }

        query.andWhere { Pile.code.inList(piles) }

        return query.map { resultRow ->
            PileEntry(
                id = resultRow[Pile.id].value,
                code = resultRow[Pile.code],
                goodMovementId = resultRow[Pile.goodMovementId].value,
                originGoodMovementId = resultRow[Pile.originGoodMovementId],
                originDepartmentId = null,
                currentDepartmentId = resultRow[GoodMovement.departmentId].value,
                currentStoreLocation = null,
                processTypePrefix = resultRow[Pile.processTypePrefix],
                manufacturingLineId = resultRow[GoodMovement.manufacturingLineId],
                productionDate = resultRow[GoodMovement.productionDate],
                orderNo = resultRow[Pile.orderNo],
                jobNo = resultRow[GoodMovement.jobNo],
                currentjobNo = resultRow[GoodMovement.jobNo],
                poNo = resultRow[GoodMovement.poNo],
                invoiceNo = resultRow[GoodMovement.invoiceNo],
                lotNo = resultRow[GoodMovement.lotNo],
                manufacturingLinename = resultRow[ManufacturingLine.name],
                suppliername = resultRow[Supplier.name],
                remark = resultRow[Pile.remark],
                sku = null,
                type = null,
                fsc = null,
                creator = resultRow[User.firstname]+" "+resultRow[User.lastname],
                printedAt = resultRow[Pile.printedAt],
                details = null,
                lots = null,
                canEdit = false,
                canRemove = false,
                canUndo = false,
                preferredMeasurement = null,
                createdAt = resultRow[Pile.createdAt]?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                matCode = null,
                grade = null,
                customer = resultRow[Pile.extraAttributes]?.get("customer") ?: " ",
                countryOfOrigin = resultRow[GoodMovement.extraAttributes]?.get("countryOfOrigin") ?: " ",
            )
        }
    }

    fun getPileAndItemCount(goodMovementId: Int): Pair<Int, Int> {
        val joins = GmItem.join(GoodMovement, JoinType.INNER) { GmItem.goodMovementId eq GoodMovement.id }
            .join(LotNo, JoinType.INNER) { LotNo.id eq GmItem.lotNoId }
            .join(PileHasLotNo, JoinType.LEFT) { PileHasLotNo.lotNoId eq GmItem.lotNoId }
        val query = joins.select(PileHasLotNo.pileId, GmItem.qty.sum())
            .where { (GmItem.status eq "A") and (LotNo.status eq "A") and (GoodMovement.id eq goodMovementId) }
            .groupBy(PileHasLotNo.pileId)
        val results = query.map { resultRow -> Pair(resultRow[PileHasLotNo.pileId], resultRow[GmItem.qty.sum()]) }
        return Pair(results.size, results.sumOf { it.second!!.toInt() })
    }

    fun updatedPilePrintedAt(piles: List<String>){
        // Update query
        val now = LocalDateTime.now()
        val pileList = piles.toList()

        Pile.update({ Pile.code inList pileList }) {
            it[printedAt] = now
            it[updatedAt] = now
        }
    }

    fun findGmItems(goodMovementId: Int, lotNoIds: List<Int>): List<GmItemDao> {
        return GmItemDao.find { (GmItem.status eq "A") and (GmItem.goodMovementId eq goodMovementId) and (GmItem.lotNoId.inList(lotNoIds)) }.toList()
    }

    private fun createNewMatcode(newSkuGroupCode: String, matCode: String): String {
        return matCode.substring(0, 1) + newSkuGroupCode + matCode.substring(3)
    }

    private fun createNewMovingItem(old: MovingItem, sku: SkuDao): MovingItem {
        return MovingItem(
            lotNoId = old.lotNoId,
            lotCode = old.lotCode,
            lotRefCode = old.lotRefCode,
            skuId = sku.id.value,
            matCode = sku.matCode,
            skuName = sku.name,
            width = sku.width,
            widthUom = sku.widthUom,
            length = sku.length,
            lengthUom = sku.lengthUom,
            thickness = sku.thickness,
            thicknessUom = sku.thicknessUom,
            volumnFt3 = sku.volumnFt3,
            volumnM3 = sku.volumnM3,
            grade = sku.grade,
            fsc = sku.fsc,
            species = sku.species,
            skuGroupId = sku.skuGroupId,
            storeLocationId = old.storeLocationId,
            storeLocationCode = old.storeLocationCode,
            qty = old.qty.setScale(2, RoundingMode.HALF_UP)
        )
    }

    data class PileActionable(
        val canEdit: Boolean,
        val canRemove: Boolean,
        val canUndo: Boolean
    )

    data class PalletCollection(val pallet: PalletDao,
                                val pileLotNos: List<Pair<PileDao, List<LotNoDao>>>) {
        fun getLotNos(): List<LotNoDao> {
            return pileLotNos.map { it.second }.flatten()
        }

        fun getLotNoIds(): List<Int> {
            return getLotNos().map { it.id.value }
        }

        fun getPiles(): List<PileDao> {
            return pileLotNos.map { it.first }
        }

        fun getLotNoByPile(pile: PileDao): List<LotNoDao> {
            return pileLotNos.first { it.first.id.value == pile.id.value }.second
        }

        fun isPalletEmpty(): Boolean {
            return pileLotNos.isEmpty()
        }
    }
}
