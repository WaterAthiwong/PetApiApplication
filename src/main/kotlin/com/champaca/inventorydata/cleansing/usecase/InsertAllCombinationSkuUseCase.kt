package com.champaca.inventorydata.cleansing.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.common.ChampacaConstant.M3_TO_FT3
import com.champaca.inventorydata.databasetable.Sku
import com.champaca.inventorydata.databasetable.dao.SkuDao
import com.champaca.inventorydata.masterdata.sku.SkuRepository
import com.champaca.inventorydata.masterdata.sku.usecase.CheckMatCodeExistUseCase
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import javax.sql.DataSource

@Deprecated("Use for import existing data only")
@Service
class InsertAllCombinationSkuUseCase(
    val skuRepository: SkuRepository,
    val dataSource: DataSource
) {
    val thicknesses = listOf("0.125", "0.25", "0.375", "0.5", "0.625", "0.75", "0.875", "1", "1.25", "1.5", "1.75", "2", "2.25", "2.5", "2.75", "3", "3.25", "3.5", "3.75", "4", "4.5", "5", "5.5", "6", "7", "8", "9", "10", "11", "12")
    val widthes = listOf("0.5", "1", "1.5", "2", "2.5", "3", "3.5", "4", "4.5", "5", "5.5", "6", "6.5", "7", "7.5", "8", "8.5", "9", "9.5", "10", "10.5", "11", "11.5", "12", "12.5", "13", "13.5", "14", "14.5", "15", "16", "17", "18", "19", "20")
    val lengths = listOf("0.5", "1", "1.5", "2", "2.5", "3", "3.5", "4", "4.5", "5", "5.5", "6", "6.5", "7", "7.5", "8", "8.5", "9", "9.5", "10", "10.5", "11", "11.5", "12", "12.5", "13", "13.5", "14", "14.5", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30")
    val fsc = listOf("1", "2")
    val gradeGroups = listOf("R0" to "A", "R0" to "B", "R0" to "C", "R0" to "D", "R0" to "F",
                             "R3" to "A", "R3" to "B", "R3" to "C", "R3" to "D", "R3" to "F")

    fun execute(group: String, grade: String, running: Int) {
        Database.connect(dataSource)

        transaction {
            addLogger(ExposedInfoLogger)

            var matCodes = mutableListOf<String>()
            thicknesses.forEach { thickness ->
                widthes.forEach { width ->
                    lengths.forEach { length ->
                        fsc.forEach { fsc ->
                            val matCode = "1${group}PT${fsc}${grade}-${getMeasurementForMatCode(thickness)}X${getMeasurementForMatCode(width)}X${getMeasurementForMatCode(length)}"
                            matCodes.add(matCode)
                        }
                    }
                }
            }

            val nonMatCodes = skuRepository.findNonExistingMatCodes(matCodes)
            val matCodeMap = nonMatCodes.associateBy({it}, {1})

            var skuInfos = mutableListOf<SkuInfo>()
            var index = running + 1
            thicknesses.forEach { thickness ->
                widthes.forEach { width ->
                    lengths.forEach { length ->
                        fsc.forEach { fsc ->
                            val matCode = "1${group}PT${fsc}${grade}-${getMeasurementForMatCode(thickness)}X${getMeasurementForMatCode(width)}X${getMeasurementForMatCode(length)}"
                            if (matCodeMap.containsKey(matCode)) {
                                val volumnFt3 = (thickness.toBigDecimal() * width.toBigDecimal() * length.toBigDecimal()).divide(144.toBigDecimal(), 5, RoundingMode.HALF_UP)
                                val volumnM3 = volumnFt3.divide(M3_TO_FT3, 5, RoundingMode.HALF_UP)
                                val skuGroup = if (group == "R1") "SAWN TIMBER KD" else "SLAB KD"
                                val fscStr = if (fsc == "1") "FSC" else "NON FSC"
                                val name = "RM-${skuGroup}-PT TEAK-${fscStr}-${grade}-${getMeasurementForMatCode(thickness)}X${getMeasurementForMatCode(width)}X${getMeasurementForMatCode(length)}"
                                val skuInfo = SkuInfo(
                                    skuGroupId = if (group == "R1") 6 else 7,
                                    code = "SKU${index.toString().padStart(6, '0')}",
                                    matCode = matCode,
                                    name = name,
                                    thickness = thickness.toBigDecimal(),
                                    width = width.toBigDecimal(),
                                    length = length.toBigDecimal(),
                                    volumnM3 = volumnM3,
                                    volumnFt3 = volumnFt3,
                                    grade = grade,
                                    fsc = fsc == "1"
                                )
                                skuInfos.add(skuInfo)
                                index++
                            }
                        }
                    }
                }
            }

            Sku.batchInsert(skuInfos) {
                this[Sku.skuGroupId] = it.skuGroupId
                this[Sku.code] = it.code
                this[Sku.matCode] = it.matCode
                this[Sku.name] = it.name
                this[Sku.thickness] = it.thickness
                this[Sku.thicknessUom] = "in"
                this[Sku.width] = it.width
                this[Sku.widthUom] = "in"
                this[Sku.length] = it.length
                this[Sku.lengthUom] = "ft"
                this[Sku.circumference] = BigDecimal.ZERO
                this[Sku.circumferenceUom] = "in"
                this[Sku.volumnM3] = it.volumnM3
                this[Sku.volumnFt3] = it.volumnFt3
                this[Sku.species] = "PT"
                this[Sku.grade] = it.grade
                this[Sku.fsc] = it.fsc
                this[Sku.status] = "A"
            }

            println("Total matcodes: ${matCodes.size}")
            println("Non existing mat codes: ${nonMatCodes.size}")
        }
    }

    fun fixVolumn(minId: Int, maxId: Int) {
        Database.connect(dataSource)

        transaction {
            addLogger(ExposedInfoLogger)

            val skus = SkuDao.find { (Sku.id greater minId) and (Sku.id less maxId) }
            skus.forEach { sku ->
                Sku.update({Sku.id eq sku.id}) {
                    val ft3 = (sku.thickness * sku.width * sku.length).divide(144.toBigDecimal(), 5, RoundingMode.HALF_UP)
                    it[volumnFt3] = ft3
                    it[volumnM3] = ft3.divide(M3_TO_FT3, 5, RoundingMode.HALF_UP)
                }
            }
        }
    }

    val VALID_FLOOR = "5[A-Z0-9]{2}([A-Z]{2})[A-Z]{0,6}([12])([A-F]{0,2})-([\\d\\.]+)X([\\d\\.]+)X([\\d\\.]+)".toRegex()
    val VALID_RANGE = "5[A-Z0-9]{2}[A-Z]{2,8}[12][A-F]{0,2}-[\\d\\.]+X[\\d\\.]+X[\\d\\.]+-\\d+".toRegex()
    val VALID_FURNITURE = "5[A-Z0-9]{10}".toRegex()

    fun addFlooring(running: Int) {
        Database.connect(dataSource)

        val floors = mutableListOf<Triple<String, String, Int>>()
        val ranges = mutableListOf<Triple<String, String, Int>>()
        val furnitures = mutableListOf<Triple<String, String, Int>>()
        val others = mutableListOf<Triple<String, String, Int>>()
        readFile("C:\\reports\\fur.txt").forEach {
            val matCode = it.first
            if (matCode.matches(VALID_FLOOR)) {
                floors.add(it)
            } else if (matCode.matches(VALID_RANGE)) {
                ranges.add(it)
            } else if (matCode.matches(VALID_FURNITURE)) {
                furnitures.add(it)
            } else {
                others.add(it)
            }
        }

        var index = running + 1
        transaction {
            addLogger(ExposedInfoLogger)
            val nonExistingFloorMatCodes = skuRepository.findNonExistingMatCodes(floors.map { it.first })
            val nonExistingFloors = floors.filter { nonExistingFloorMatCodes.contains(it.first) }
            Sku.batchInsert(nonExistingFloors) {
                this[Sku.skuGroupId] = it.third
                this[Sku.code] = "SKU${(index).toString().padStart(6, '0')}"
                this[Sku.matCode] = it.first
                this[Sku.name] = it.second

                val match = VALID_FLOOR.find(it.first)
                val species = match!!.groupValues[1]
                val fsc = match!!.groupValues[2].toInt() == 1
                val grade = match!!.groupValues[3]
                val thickness = match!!.groupValues[4].toBigDecimal()
                val width = match!!.groupValues[5].toBigDecimal()
                val length = match!!.groupValues[6].toBigDecimal()

                this[Sku.thickness] = thickness
                this[Sku.thicknessUom] = "mm"
                this[Sku.width] = width
                this[Sku.widthUom] = "mm"
                this[Sku.length] = length
                this[Sku.lengthUom] = "mm"
                this[Sku.circumference] = BigDecimal.ZERO
                this[Sku.circumferenceUom] = "mm"

                val volumnM3 = thickness.multiply(width).multiply(length).divide(1000000000.0.toBigDecimal())
                val volumnFt3 = volumnM3.multiply(M3_TO_FT3)

                this[Sku.volumnM3] = volumnM3
                this[Sku.volumnFt3] = volumnFt3
                this[Sku.species] = species
                this[Sku.grade] = grade
                this[Sku.fsc] = fsc
                this[Sku.status] = "A"
                index++
            }
            println("Total Floors: ${floors.size}")
            println("Imported Floors: ${nonExistingFloors.size}")

            val nonExistingFurMatCodes = skuRepository.findNonExistingMatCodes(furnitures.map { it.first })
            val nonExistingFurs = furnitures.filter { nonExistingFurMatCodes.contains(it.first) }
            Sku.batchInsert(nonExistingFurs) {
                this[Sku.skuGroupId] = it.third
                this[Sku.code] = "SKU${(index).toString().padStart(6, '0')}"
                this[Sku.matCode] = it.first
                this[Sku.name] = it.second
                this[Sku.thickness] = BigDecimal.ZERO
                this[Sku.thicknessUom] = "mm"
                this[Sku.width] = BigDecimal.ZERO
                this[Sku.widthUom] = "mm"
                this[Sku.length] = BigDecimal.ZERO
                this[Sku.lengthUom] = "mm"
                this[Sku.circumference] = BigDecimal.ZERO
                this[Sku.circumferenceUom] = "mm"
                this[Sku.volumnM3] = BigDecimal.ZERO
                this[Sku.volumnFt3] = BigDecimal.ZERO
                this[Sku.species] = it.first.takeLast(3).dropLast(1)
                this[Sku.grade] = ""
                this[Sku.fsc] = it.first.last().toString().toInt() == 1
                this[Sku.status] = "A"
                index++
            }
            println("Total Furnitures: ${furnitures.size}")
            println("Imported Furnitures: ${nonExistingFurs.size}")
        }
        println("Ranges:")
        println(ranges.map { it.first }.joinToString("\n"))
        println("Non Conform:")
        println(others.map { it.first }.joinToString("\n"))
    }

    private fun readFile(filePath: String): List<Triple<String, String, Int>> {
        var results = mutableListOf<Triple<String, String, Int>>()
        try {
            // Read lines from the file and store them in a list
            val lines: List<String> = File(filePath).readLines()

            // Now 'lines' contains each line of the file as an element of the list
            // You can process the list as needed
            lines.forEach { line ->
                val items = line.split(",")
                results.add(Triple(items[0].trim(), items[1].trim(), items[2].trim().toInt()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    private fun getMeasurementForMatCode(measurement: String): String {
        if (!measurement.matches(CheckMatCodeExistUseCase.FRACTION)) {
            return measurement
        }

        val dotPos = measurement.indexOf('.')
        val integer = measurement.substring(0, dotPos)


        // All available fraction are 1/2, 1/3, 1/4, 3/4, 1/5, 2/5, 3/5, 4/5, 1/7, 2/7, 5/7, 1/8, 5/8, 7/8, 5/9, 4/9,
        // The majority of fraction are 1/2 (13755 records), 3/4 (4922 records), 1/4 (2270 records) 5/8 (72 records), the rest are minor
        // the unlikely ones are 1/7 (1 record), 2/7 (1), 5/7 (11), 4/9 (1), 5/9 (2) will be omitted for now
        val fraction = when(measurement.substring(dotPos + 1)) {
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

    data class SkuInfo(
        val skuGroupId: Int,
        val code: String,
        val matCode: String,
        val name: String,
        val thickness: BigDecimal,
        val width: BigDecimal,
        val length: BigDecimal,
        val volumnM3: BigDecimal,
        val volumnFt3: BigDecimal,
        val grade: String,
        val fsc: Boolean
    )
}