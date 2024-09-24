package com.champaca.inventorydata.masterdata.sku.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.dao.SkuDao
import com.champaca.inventorydata.masterdata.sku.SkuRepository
import com.champaca.inventorydata.masterdata.sku.model.MatCodeQuery
import com.champaca.inventorydata.masterdata.sku.response.CheckMatCodeExistResponse
import com.champaca.inventorydata.masterdata.skugroup.SkuGroupRepository
import com.champaca.inventorydata.model.Species
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class CheckMatCodeExistUseCase(
    val dataSource: DataSource,
    val skuRepository: SkuRepository,
    val skuGroupRepository: SkuGroupRepository
) {

    companion object {
        val FRACTION = "\\d+\\.\\d+".toRegex()
    }

    val logger = LoggerFactory.getLogger(CheckMatCodeExistUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(matCodeQuery: MatCodeQuery): CheckMatCodeExistResponse {
        var matCode = ""

        Database.connect(dataSource)
        var sku: SkuDao? = null
        transaction {
            addLogger(exposedLogger)
            matCode = getMatCode(matCodeQuery)
            sku = skuRepository.findByMatCode(matCode)

            if (sku == null) {
                matCode = getMatCode(matCodeQuery, useSimpleMeasurement = true)
                sku = skuRepository.findByMatCode(matCode)
            }
        }
        val isValid = sku != null
        if (!isValid) {
            return CheckMatCodeExistResponse(isValid = false)
        }

        return CheckMatCodeExistResponse(isValid = true, matCode = matCode, skuId = sku!!.id.value)
    }

    val BY_INCHES = listOf<String>("PT", "NT")

    private fun getMatCode(item: MatCodeQuery, useSimpleMeasurement: Boolean = false): String {
        if (item.skuGroupCode == "L0") {
            // อันนี้เป็นวิธีแก้ง่ายๆ สำหรับ L0 ที่เป็นเรียกมาจากคลังแปรรูป เพราะของคลังแปรรูป
            // ทางฝั่ง frontend (logs.champaca) จะ hardcode maingroup มาเป็น RM
            item.mainGroup = "COMPONENT"
        }

        // e.g. 1R0PT1-1X2X1, 1R0PT1A-1X3X12, 1R0PT1B-1X6X7 1/2
        val fsc = if (item.fsc) 1 else 2
        val measurement = if (!useSimpleMeasurement) getMeasurement(item) else getSimpleMeasurement(item)
        val skuGroup = skuGroupRepository.getAll().find { (it.erpGroupCode == item.skuGroupCode) and (it.erpMainGroupName == item.mainGroup) }!!
        val species = getSpecies(item)
        val matCode = "${skuGroup.erpMainGroupId}${item.skuGroupCode}${species}${fsc}${item.grade}-${measurement}"
        logger.info( "Mat Code: $matCode")
        return matCode
    }

    private fun getMeasurement(item: MatCodeQuery): String {
        if (item.mainGroup == "RM") {
            // This is for RM matcode, e.g. 1R0PT1-1X2X1, 1R0PT1A-1X3X12, 1R0PT1B-1X6X7 1/2
            val thickness = getMeasurementForMatCode(item.thickness)
            val width = getMeasurementForMatCode(item.width)
            val length = if (BY_INCHES.contains(item.species)) {
                getMeasurementForMatCode(item.length)
            } else {
                val result = item.length.toBigDecimal().setScale(2).toString()
                if (result.endsWith(".00")) {
                    result.substring(0, result.length - 3)
                } else {
                    result
                }
            }
            return "${thickness}X${width}X${length}"
        } else {
            // This is for other main group where the measurement are not inch-based. e.g. 3L3DSTETE2AB-14.30X148X1530, 5L3OATETE2-14X300X1600
            return getSimpleMeasurement(item)
        }
    }

    private fun getSimpleMeasurement(item: MatCodeQuery): String {
        val thickness = fillLastZero(item.thickness)
        val width = fillLastZero(item.width)
        val length = fillLastZero(item.length)
        return "${thickness}X${width}X${length}"
    }

    private fun fillLastZero(measurement: String): String {
        if (measurement.contains('.')) {
            val dotPos = measurement.indexOf('.')
            val integer = measurement.substring(0, dotPos)
            val fraction = measurement.substring(dotPos + 1)
            if (fraction.length == 1) {
                return "$integer.$fraction" + "0"
            }
        }
        return measurement
    }

    private fun getSpecies(item: MatCodeQuery): String {
        if (item.secondLayerSpecies.isNullOrEmpty() && item.thirdLayerSpecies.isNullOrEmpty() && item.fourthLayerSpecies.isNullOrEmpty()) {
            return item.species
        }
        return "${item.species}${item.secondLayerSpecies ?: ""}${item.thirdLayerSpecies ?: ""}${item.fourthLayerSpecies ?: ""}"
    }

    private fun getMeasurementForMatCode(measurement: String): String {
        if (!measurement.matches(FRACTION)) {
            return measurement
        }

        val dotPos = measurement.indexOf('.')
        val integer = measurement.substring(0, dotPos)
        val fractionStr = measurement.substring(dotPos + 1)

        // All available fraction are 1/2, 1/3, 1/4, 3/4, 1/5, 2/5, 3/5, 4/5, 1/7, 2/7, 5/7, 1/8, 5/8, 7/8, 5/9, 4/9,
        // The majority of fraction are 1/2 (13755 records), 3/4 (4922 records), 1/4 (2270 records) 5/8 (72 records), the rest are minor
        // the unlikely ones are 1/7 (1 record), 2/7 (1), 5/7 (11), 4/9 (1), 5/9 (2) will be omitted for now
        val fraction = when(fractionStr) {
            // Of second
            "5", "50"   -> "1/2"

            // Of third
            "33"        -> "1/3"
            "66"        -> "2/3"

            // Of fourth
            "25"        -> "1/4"
            "75"        -> "3/4"

            // Of fifth
            "2", "20"   -> "1/5"
            "4", "40"   -> "2/5"
            "6", "60"   -> "3/5"
            "8", "80"   -> "4/5"

            // Of eighth
            "125"       -> "1/8"
            "375"       -> "1/8"
            "625"       -> "5/8"
            "875"       -> "7/8"
            else -> ""
        }
        return if (integer == "0") {
            fraction.trim()
        } else {
            "$integer $fraction".trim()
        }
    }
}