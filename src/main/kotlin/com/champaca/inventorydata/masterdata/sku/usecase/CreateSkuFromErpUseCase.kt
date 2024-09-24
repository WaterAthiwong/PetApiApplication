package com.champaca.inventorydata.masterdata.sku.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ChampacaConstant.M3_TO_FT3
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.SkuRequest
import com.champaca.inventorydata.databasetable.dao.SkuDao
import com.champaca.inventorydata.databasetable.dao.SkuGroupDao
import com.champaca.inventorydata.masterdata.sku.SkuRepository
import com.champaca.inventorydata.masterdata.sku.request.CreateSkuFromErpRequest
import com.champaca.inventorydata.masterdata.skugroup.SkuGroupRepository
import com.champaca.inventorydata.model.SkuDetailImpl
import com.champaca.inventorydata.wms.WmsService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class CreateSkuFromErpUseCase(
    val dataSource: DataSource,
    val skuGroupRepository: SkuGroupRepository,
    val skuRepository: SkuRepository,
    val wmsService: WmsService
) {

    companion object {
        const val GROUP_CODE_LOG = "M0"
        const val METER = "เมตร"
        const val FOOT = "ฟุต"
        const val MILL = "มิลลิเมตร"
        const val INCH = "นิ้ว"
        val AVAILABLE_UOMS = listOf(METER, FOOT, MILL, INCH)
    }

    val logger = LoggerFactory.getLogger(CreateSkuFromErpUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, userId: String, request: CreateSkuFromErpRequest): ResultOf<String> {
        Database.connect(dataSource)

        var errorMessage = ""
        var skuGroups: List<SkuGroupDao> = listOf()
        var sku: SkuDao? = null
        transaction {
            addLogger(exposedLogger)
            skuGroups = skuGroupRepository.getAll()
            sku = skuRepository.findByMatCode(request.matCode ?: "")
        }

        val validationResult = validateRequest(request, skuGroups, sku)
        if (validationResult is ResultOf.Failure) {
            errorMessage = validationResult.message!!
        } else {
            val wmsResult = createSkuInWms(sessionId, request, skuGroups)
            if (wmsResult is ResultOf.Failure) {
                errorMessage = wmsResult.message!!
            }
        }

        transaction {
            addLogger(exposedLogger)
            SkuRequest.insert {
                it[this.userId] = userId.toInt()
                it[this.payload] = Json.encodeToString(request)
                it[this.success] = errorMessage.isNullOrEmpty()
                it[this.error] = errorMessage
                it[this.createdAt] = LocalDateTime.now()
            }
        }

        return if(errorMessage.isNullOrEmpty()) ResultOf.Success("") else ResultOf.Failure(message = errorMessage)
    }

    private fun validateRequest(request: CreateSkuFromErpRequest,
                                skuGroups: List<SkuGroupDao>,
                                sku: SkuDao?): ResultOf<String> {
        val skuGroupCodes = skuGroups.map { it.erpGroupCode }
        if (!skuGroupCodes.contains(request.productGroup)) {
            return ResultOf.Failure("ProductGroupNotFound")
        }

        if (sku != null) {
            return ResultOf.Failure("ExistingSKU")
        }

        if (request.rowOrder.isNullOrEmpty()) {
            return ResultOf.Failure("NoRowOrder")
        }

        if (request.name.isNullOrEmpty()) {
            return ResultOf.Failure("NoName")
        }

        if (request.matCode.isNullOrEmpty()) {
            return ResultOf.Failure("NoMatCode")
        }

        if (request.thickness != null && request.thicknessUom.isNullOrEmpty()) {
            return ResultOf.Failure("NoThicknessUOM")
        } else if (!AVAILABLE_UOMS.contains(request.thicknessUom)) {
            return ResultOf.Failure("IncompatibleThicknessUOM")
        }

        if (request.width != null && request.widthUom.isNullOrEmpty()) {
            return ResultOf.Failure("NoWidthUOM")
        } else if (!AVAILABLE_UOMS.contains(request.widthUom)) {
            return ResultOf.Failure("IncompatibleWidthUOM")
        }

        if (request.length != null && request.lengthUom.isNullOrEmpty()) {
            return ResultOf.Failure("NoLengthUOM")
        } else if (!AVAILABLE_UOMS.contains(request.lengthUom)) {
            return ResultOf.Failure("IncompatibleLengthUOM")
        }

        if (request.species.isNullOrEmpty()) {
            return ResultOf.Failure("NoSpecies")
        }

        if (request.fsc.isNullOrEmpty()) {
            return ResultOf.Failure("NoFSC")
        }

        if (request.volumnFt3 == null) {
            return ResultOf.Failure("NoVolumnFt3")
        }

        return ResultOf.Success("")
    }

    private fun getUom(uom: String?): String? {
        return when(uom) {
            METER -> "m"
            FOOT -> "ft"
            MILL -> "mm"
            INCH -> "in"
            else -> null
        }
    }

    private fun createSkuInWms(sessionId: String, request: CreateSkuFromErpRequest,
                               skuGroups: List<SkuGroupDao>): ResultOf<String> {
        val circumference = if (request.productGroup == GROUP_CODE_LOG) request.width.toBigDecimal().setScale(2) else (0.0).toBigDecimal()
        val circumferenceUom = "m"
        val width = if (request.productGroup == GROUP_CODE_LOG) (0.0).toBigDecimal() else request.width.toBigDecimal().setScale(2)
        val thicknessUom = getUom(request.thicknessUom) ?: "m"
        val widthUom = getUom(request.widthUom) ?: "m"
        val lengthUom = getUom(request.lengthUom) ?: "m"
        val volumnM3 = request.volumnFt3!!.toBigDecimal().setScale(5) / M3_TO_FT3
        val fsc = request.fsc == "FSC"
        val skuGroupId = skuGroups.first { it.erpGroupCode == request.productGroup }.id.value

        val skuDetail = SkuDetailImpl(
            matCode = request.matCode!!,
            skuName = request.name!!,
            width = width,
            widthUom = widthUom,
            length = request.length.toBigDecimal().setScale(2),
            lengthUom = lengthUom,
            thickness = request.thickness.toBigDecimal().setScale(2),
            thicknessUom = thicknessUom,
            circumference = circumference,
            circumferenceUom = circumferenceUom,
            volumnFt3 = request.volumnFt3!!.toBigDecimal().setScale(2),
            volumnM3 = volumnM3,
            areaM2 = BigDecimal.ZERO,
            grade = request.grade,
            fsc = fsc,
            species = request.species!!,
        ).apply {
            this.code = request.rowOrder!!
        }

        return wmsService.createSku(sessionId, skuDetail, skuGroupId)
    }
}