package com.champaca.inventorydata.pile.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.GmItem
import com.champaca.inventorydata.databasetable.GoodMovement
import com.champaca.inventorydata.databasetable.LotNo
import com.champaca.inventorydata.databasetable.dao.GoodMovementDao
import com.champaca.inventorydata.databasetable.dao.PileDao
import com.champaca.inventorydata.databasetable.dao.SkuGroupDao
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.masterdata.skugroup.SkuGroupRepository
import com.champaca.inventorydata.model.Species
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.response.GetLotsResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetLotsUseCase(
    val dataSource: DataSource,
    val pileService: PileService,
    val skuGroupRepository: SkuGroupRepository
) {
    val logger = LoggerFactory.getLogger(GetLotsUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(pileCode: String, itemOnly: Boolean = false): GetLotsResponse {
        Database.connect(dataSource)

        var errorType = PileError.NONE
        var errorMessage = ""
        var pickingItems: List<MovingItem> = listOf()
        lateinit var pile: PileDao
        lateinit var firstItem: MovingItem
        lateinit var skuGroup: SkuGroupDao
        transaction {
            addLogger(exposedLogger)
            val pair = pileService.findPileAndCurrentLotNos(pileCode)
            if (pair == null) {
                errorType = PileError.PILE_NOT_FOUND
                return@transaction
            }

            pile = pair.first
            val lotNoIds = pair.second.map { it.id.value }
            pickingItems = pileService.findItemsInStorageArea(lotNoIds)
            if (pickingItems.isEmpty()) {
                errorType = PileError.LOT_HAS_ZERO_AMOUNT
                val goodMovement = findPickingGoodMovement(lotNoIds)
                if (goodMovement != null) {
                    errorMessage = goodMovement.code
                }
                return@transaction
            }

            if (itemOnly) {
                pickingItems = squashPickingItems(pickingItems)
            }

            firstItem = pickingItems.first()
            skuGroup = skuGroupRepository.getAll().first { it.id.value == firstItem.skuGroupId }
        }

        if (errorType != PileError.NONE) {
            return GetLotsResponse.Failure(errorType=errorType, errorMessage=errorMessage)
        }

        val species = firstItem.matCode.substring(3, 5)
        val fsc = firstItem.matCode.substring(5, 6) == "1"
        val matCode = firstItem.matCode
        val skuMainGroupName = firstItem.skuMainGroupName

        return GetLotsResponse.Success(pileId = pile.id.value,
                pileCode = pileCode,
                goodMovementId = pile.goodMovementId.value,
                lots = pickingItems,
                skuGroup = GetLotsResponse.NameIdPair(code = skuGroup.erpGroupCode, name = "${skuGroup.erpGroupCode} ${skuGroup.erpGroupName}"),
                species = GetLotsResponse.NameIdPair(code = species, name = Species.valueOf(species).longName),
                fsc = fsc,
                orderNo = pile.orderNo,
                refNo = pile.extraAttributes?.get("refNo"),
                customer = pile.extraAttributes?.get("customer"),
                remark = pile.remark,
                matCode = matCode,
                skuMainGroupName = skuMainGroupName,
                thicknesses = pickingItems.map { it.thickness }.distinct().sorted(),
                widths = pickingItems.map { it.width }.distinct().sorted(),
                lengths = pickingItems.map { it.length }.distinct().sorted(),
                grades = pickingItems.map { it.grade ?: "" }.distinct().sorted(),
                pileType = pile.type,
            )
    }

    private fun findPickingGoodMovement(lotNoIds: List<Int>): GoodMovementDao? {
        val joins = GmItem.join(LotNo, JoinType.INNER) { GmItem.lotNoId eq LotNo.id }
            .join(GoodMovement, JoinType.INNER) { (GoodMovement.id eq GmItem.goodMovementId) and
                    (GoodMovement.type eq GoodMovementType.PICKING_ORDER.wmsName)}
        val query = joins.select(GoodMovement.columns)
            .where { (LotNo.status eq "A") and (GmItem.status eq "A") and (GoodMovement.status eq "A") and
                    (LotNo.id inList lotNoIds) }
        val results = GoodMovementDao.wrapRows(query).toList()
        return if (results.isEmpty()) null else results.first()
    }

    private fun squashPickingItems(pickingItems: List<MovingItem>): List<MovingItem> {
        return pickingItems.groupBy { it.matCode }
            .mapValues { (_, items) ->
                val firstItem = items.first()
                MovingItem(
                    lotNoId = -1,
                    lotCode = "",
                    lotRefCode = "",
                    skuId = firstItem.skuId,
                    skuGroupId = firstItem.skuGroupId,
                    matCode = firstItem.matCode,
                    skuName = firstItem.skuName,
                    width = firstItem.width,
                    widthUom = firstItem.widthUom,
                    length = firstItem.length,
                    lengthUom = firstItem.lengthUom,
                    thickness = firstItem.thickness,
                    thicknessUom = firstItem.thicknessUom,
                    volumnFt3 = firstItem.volumnFt3,
                    volumnM3 = firstItem.volumnM3,
                    grade = firstItem.grade,
                    fsc = firstItem.fsc,
                    species = firstItem.species,
                    storeLocationId = firstItem.storeLocationId,
                    storeLocationCode = firstItem.storeLocationCode,
                    qty = items.sumOf { it.qty }
                )
            }
            .values.toList()
    }
}