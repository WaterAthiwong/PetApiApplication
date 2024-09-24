package com.champaca.inventorydata.odoo.usecase

import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.Sku
import com.champaca.inventorydata.masterdata.sku.SkuRepository
import com.champaca.inventorydata.masterdata.sku.model.SkuData
import com.champaca.inventorydata.masterdata.skugroup.SkuGroupRepository
import com.champaca.inventorydata.odoo.OdooService
import com.champaca.inventorydata.odoo.request.UpsertSkuRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class UpsertSkuUseCase(
    val dataSource: DataSource,
    val skuGroupRepository: SkuGroupRepository,
    val odooService: OdooService,
    val skuRepository: SkuRepository,
    val cacheManager: CacheManager
) {
    companion object {
        val LOG_REGEX = "(\\d)([A-Z0-9]{2})([A-Z]{2})(\\d)-(\\d+)X([0-9.]+)".toRegex()
        val WIP_REGEX = "(\\d)([A-Z0-9]{2})([A-Z]{2,8})(\\d)([A-Z]+)?-([0-9.]+)X([0-9.]+)X([0-9.]+)".toRegex()
        val FLOOR_FG_REGEX = "(\\d)([A-Z0-9]{2})([A-Z]{2,8})(\\d)([A-Z]+)?-([0-9.]+)X([0-9.]+)X([0-9.]+)-([A-Z0-9]{5})-([A-Z0-9]{4})".toRegex()
    }

    val logger = LoggerFactory.getLogger(UpsertSkuUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun create(request: UpsertSkuRequest) {
        val payload = Json.encodeToString(request)
        logger.info("Payload: $payload")
        Database.connect(dataSource)
        transaction {
            addLogger(exposedLogger)
            val skuType = getType(request.matCode)
            if (skuType == SkuType.UNKNOWN) {
                logger.info("Unknown sku type for mat code ${request.matCode}")
                return@transaction
            }

            val skuData = getSkuData(request, skuType)
            if (skuData == null) {
                logger.error("No product group found in WMS for ${request.matCode}")
                odooService.recordError("CreateSku", payload, "No product group found in WMS for ${request.matCode}")
                return@transaction
            }

            val sku = skuRepository.findByMatCode(request.matCode)
            if (sku != null) {
                logger.error("SKU with mat code ${request.matCode} already exists")
                odooService.recordError("CreateSku", payload, "SKU with mat code ${request.matCode} already exists")
                return@transaction
            }

            val newSkuId = insertSku(request, skuData)
            logger.info("SKU with mat code ${request.matCode} created with id $newSkuId, Odoo id ${request.odooId}")
        }
    }

    fun update(request: UpsertSkuRequest) {
        val payload = Json.encodeToString(request)
        logger.info("Payload: $payload")
        Database.connect(dataSource)
        transaction {
            addLogger(exposedLogger)
            val skuType = getType(request.matCode)
            if (skuType == SkuType.UNKNOWN) {
                logger.info("Unknown sku type for mat code ${request.matCode}")
                return@transaction
            }

            val skuData = getSkuData(request, skuType)
            if (skuData == null) {
                logger.error("No product group found in WMS for ${request.matCode}")
                odooService.recordError("UpdateSku", payload, "No product group found in WMS for ${request.matCode}")
                return@transaction
            }

            val sku = skuRepository.findByMatCode(request.matCode)
            if (sku != null) {
                updateSku(request, sku.id.value, skuData)
                logger.info("SKU with mat code ${request.matCode} updated, Odoo id ${request.odooId}")
            } else if(skuType == SkuType.FLOOR_FG) {
                val newSkuId = insertSku(request, skuData)
                logger.info("SKU with mat code ${request.matCode} created with id $newSkuId, Odoo id ${request.odooId}")
            }
        }
    }

    private fun getType(matCode: String?): SkuType {
        if (matCode.isNullOrEmpty()) {
            return SkuType.UNKNOWN
        }
        return when {
            LOG_REGEX.matches(matCode) -> SkuType.LOG
            WIP_REGEX.matches(matCode) -> SkuType.WIP
            FLOOR_FG_REGEX.matches(matCode) -> SkuType.FLOOR_FG
            else -> SkuType.UNKNOWN
        }
    }

    private fun getSkuData(request: UpsertSkuRequest, skuType: SkuType): SkuData? {
        var mainGroup = 0
        var subGroup = ""
        var circumference = BigDecimal.ZERO
        var thickness = BigDecimal.ZERO
        var width = BigDecimal.ZERO
        var length = BigDecimal.ZERO
        var circumferenceUom = "mm"
        var thicknessUom = "mm"
        var widthUom = "mm"
        var lengthUom = "mm"
        var grade = ""
        var species = ""
        var fsc = ""
        var extraAttributes = mapOf<String, String>()

        when(skuType) {
            SkuType.LOG -> {
                val group = LOG_REGEX.find(request.matCode)!!.groupValues
                mainGroup = group[1].toInt()
                subGroup = group[2]
                species = group[3]
                fsc = group[4]
                circumference = group[5].toBigDecimal()
                circumferenceUom = "cm"
                length = group[6].toBigDecimal()
                lengthUom = "m"
            }
            SkuType.WIP -> {
                val group = WIP_REGEX.find(request.matCode)!!.groupValues
                mainGroup = group[1].toInt()
                subGroup = group[2]
                species = group[3].substring(0, 2)
                fsc = group[4]
                grade = group[5]
                thickness = group[6].toBigDecimal()
                width = group[7].toBigDecimal()
                length = group[8].toBigDecimal()
            }
            SkuType.FLOOR_FG -> {
                val group = WIP_REGEX.find(request.matCode)!!.groupValues
                mainGroup = group[1].toInt()
                subGroup = group[2]
                species = group[3].substring(0, 2)
                fsc = group[4]
                grade = group[5]
                thickness = group[6].toBigDecimal()
                width = group[7].toBigDecimal()
                length = group[8].toBigDecimal()
                extraAttributes = mutableMapOf()
                if (request.extraAttributes.containsKey("Color")) {
                    extraAttributes["color"] = request.extraAttributes["Color"]!!
                }
                if (request.extraAttributes.containsKey("Pattern")) {
                    extraAttributes["pattern"] = request.extraAttributes["Pattern"]!!
                }
                if (request.extraAttributes.containsKey("Texture")) {
                    extraAttributes["texture"] = request.extraAttributes["Texture"]!!
                }
                if (request.extraAttributes.containsKey("Coating")) {
                    extraAttributes["coating"] = request.extraAttributes["Coating"]!!
                }
            }
            else -> {}
        }
        val skuGroup = skuGroupRepository.getAll().filter { (it.erpMainGroupId == mainGroup) && (it.erpGroupCode == subGroup) }
            .firstOrNull()
        if (skuGroup == null) {
            return null
        }

        return SkuData(
            skuGroupId = skuGroup.id.value,
            skuGroupName = "${skuGroup.erpMainGroupId}${skuGroup.erpGroupCode}",
            code = request.matCode,
            matCode = request.matCode,
            name = request.name,
            thickness = thickness,
            width = width,
            length = length,
            widthUom = widthUom,
            lengthUom = lengthUom,
            thicknessUom = thicknessUom,
            circumference = circumference,
            circumferenceUom = circumferenceUom,
            volumnM3 = request.volumnM3,
            volumnFt3 = request.volumnFt3,
            areaM2 = request.areaM2,
            species = species,
            grade = grade,
            fsc = fsc,
            extraAttributes = extraAttributes
        )
    }

    private fun insertSku(request: UpsertSkuRequest, skuData: SkuData): Int {
        val now = LocalDateTime.now()
        val id = Sku.insertAndGetId {
            it[Sku.skuGroupId] = skuData.skuGroupId
            it[Sku.erpCode] = request.odooId.toString()
            it[Sku.code] = skuData.code
            it[Sku.matCode] = skuData.matCode
            it[Sku.name] = skuData.name
            it[Sku.thickness] = skuData.thickness
            it[Sku.width] = skuData.width
            it[Sku.length] = skuData.length
            it[Sku.widthUom] = skuData.widthUom
            it[Sku.lengthUom] = skuData.lengthUom
            it[Sku.thicknessUom] = skuData.thicknessUom
            it[Sku.circumference] = skuData.circumference
            it[Sku.circumferenceUom] = skuData.circumferenceUom
            it[Sku.volumnM3] = skuData.volumnM3
            it[Sku.volumnFt3] = skuData.volumnFt3
            it[Sku.areaM2] = skuData.areaM2
            it[Sku.species] = skuData.species
            it[Sku.grade] = skuData.grade ?: ""
            it[Sku.fsc] = skuData.fsc == "1"
            if (skuData.extraAttributes.isNotEmpty()) {
                it[Sku.extraAttributes] = skuData.extraAttributes
            }
            it[Sku.createdAt] = now
            it[Sku.updatedAt] = now
            it[Sku.status] = "A"
        }
        cacheManager.getCache("skusByMatCode")?.evict(request.matCode)
        return id.value
    }

    private fun updateSku(request: UpsertSkuRequest, skuId: Int, skuData: SkuData) {
        Sku.update({ Sku.id eq skuId }) {
            it[Sku.skuGroupId] = skuData.skuGroupId
            it[Sku.erpCode] = request.odooId.toString()
            it[Sku.code] = skuData.code
            it[Sku.matCode] = skuData.matCode
            it[Sku.name] = skuData.name
            it[Sku.thickness] = skuData.thickness
            it[Sku.width] = skuData.width
            it[Sku.length] = skuData.length
            it[Sku.widthUom] = skuData.widthUom
            it[Sku.lengthUom] = skuData.lengthUom
            it[Sku.thicknessUom] = skuData.thicknessUom
            it[Sku.circumference] = skuData.circumference
            it[Sku.circumferenceUom] = skuData.circumferenceUom
            it[Sku.volumnM3] = skuData.volumnM3
            it[Sku.volumnFt3] = skuData.volumnFt3
            it[Sku.areaM2] = skuData.areaM2
            it[Sku.species] = skuData.species
            it[Sku.grade] = skuData.grade ?: ""
            it[Sku.fsc] = skuData.fsc == "1"
            if (skuData.extraAttributes.isNotEmpty()) {
                it[Sku.extraAttributes] = skuData.extraAttributes
            }
            it[Sku.updatedAt] = LocalDateTime.now()
        }
        cacheManager.getCache("skusByMatCode")?.evict(request.matCode)
    }

    enum class SkuType {
        LOG, WIP, FLOOR_FG, UNKNOWN
    }
}