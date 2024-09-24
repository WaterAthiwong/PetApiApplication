package com.champaca.inventorydata.masterdata.sku.excel

import com.champaca.inventorydata.common.ChampacaConstant.M3_TO_FT3
import com.champaca.inventorydata.masterdata.sku.model.SkuData
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.math.BigDecimal

@Service
class ErpExportReader {
    companion object {
        const val ROW_ORDER = 0
        const val ITEM_CODE = 1
        const val PRODUCT_GROUP = 9
        const val WOOD_TYPE = 12
        const val CERTIFICATION = 15
        const val ITEM_NAME = 37
        const val THICK = 69
        const val WIDE = 70
        const val LENGTH = 71
        const val GRADE = 63

        const val GROUP_CODE_LOG = "M0"
        const val GROUP_CODE_SAWN_TIMBER = "R0"
        const val GROUP_CODE_SLAB = "R3"
        const val GROUP_CODE_SLAB_KD = "R4"
        const val GROUP_CODE_SAWN_TIMBER_ACQ = "R9"
        const val GROUP_CODE_SAWN_TIMBER_KD_ACQ = "R2"
        const val GROUP_CODE_SAWN_TIMBER_KD = "R1"
        const val GROUP_CODE_PLYWOOD = "R7"
        val FABRICATED_GROUPS = listOf(
            GROUP_CODE_SAWN_TIMBER, GROUP_CODE_SAWN_TIMBER_KD, GROUP_CODE_SAWN_TIMBER_KD_ACQ,
            GROUP_CODE_SLAB, GROUP_CODE_SLAB_KD, GROUP_CODE_SAWN_TIMBER_ACQ
        )

        const val NATURAL_TEAK = "NT"
        const val PLANTED_TEAK = "PT"
    }

    fun readFile(filePath: String, skuGroupIds: Map<String, Int>): List<SkuData> {
        // Reference: https://chercher.tech/kotlin/read-write-excel-kotlin
        val inputStream = FileInputStream(filePath)
        var xlWb = WorkbookFactory.create(inputStream)
        val xlWs = xlWb.getSheetAt(0)

        // Reference for using physicalNumberOfRows https://www.baeldung.com/java-excel-find-last-row#2-using-getphysicalnumberofrows
        val xlRows = xlWs.physicalNumberOfRows - 1
        var startIndex = 1;
        val skuDatas = mutableListOf<SkuData>()
        for(index in startIndex..xlRows) {
            val row = xlWs.getRow(index)
            val matCode = row.getCell(ITEM_CODE).stringCellValue
            val productGroup = row.getCell(PRODUCT_GROUP).stringCellValue
            val species = row.getCell(WOOD_TYPE).stringCellValue
            val fsc = if(row.getCell(CERTIFICATION).stringCellValue == "FSC")  "Y" else "N"
            val name = row.getCell(ITEM_NAME).stringCellValue
            val grade = row.getCell(GRADE)?.stringCellValue ?: ""
            val thickness = row.getCell(THICK).numericCellValue.toBigDecimal()
            val length = row.getCell(LENGTH).numericCellValue.toBigDecimal()
            val skuGroupId = if(skuGroupIds.containsKey(productGroup)) skuGroupIds[productGroup]!! else -1

            var width = BigDecimal.ZERO
            var circumference = BigDecimal.ZERO
            var circumferenceUom = ""
            var widthUom = "NA"
            var lengthUom = "NA"
            var thicknessUom = "NA"

            var volumnFt3 = BigDecimal.ZERO
            var volumnM3 = BigDecimal.ZERO
            // In case the item is timber, the circumference value is in WIDE column and the width value is 0.
            if(species == GROUP_CODE_LOG) {
                width = BigDecimal.ZERO
                circumference = row.getCell(WIDE).numericCellValue.toBigDecimal()

                widthUom = "NA"
                circumferenceUom = "cm"
                lengthUom = "m"
                thicknessUom = "NA"

                calculateLogVolumn(circumference, circumferenceUom, length, lengthUom).let {
                    volumnFt3 = it.first
                    volumnM3 = it.second
                }
            }
            else {
                width = row.getCell(WIDE).numericCellValue.toBigDecimal()
                circumference = BigDecimal.ZERO
                circumferenceUom = "NA"
                determineUom(productGroup, species).let {
                    thicknessUom = it.first
                    widthUom = it.second
                    lengthUom= it.third
                }

                calculateProcessedWoodVolumn(
                    thickness = thickness,
                    thicknessUom = thicknessUom,
                    width = width,
                    widthUom = widthUom,
                    length = length,
                    lengthUom = lengthUom
                ).let {
                    volumnFt3 = it.first
                    volumnM3 = it.second
                }
            }

            skuDatas.add(
                SkuData(
                    skuGroupId = skuGroupId,
                    skuGroupName = productGroup,
                    code = "SKU000000001",
                    matCode = matCode,
                    name = name,
                    thickness = thickness,
                    thicknessUom = thicknessUom,
                    width = width,
                    widthUom = widthUom,
                    length = length,
                    lengthUom = lengthUom,
                    circumference = circumference,
                    circumferenceUom = circumferenceUom,
                    volumnFt3 = volumnFt3,
                    volumnM3 = volumnM3,
                    areaM2 = BigDecimal.ZERO,
                    species = species,
                    grade = grade,
                    fsc = fsc
                )
            )
        }

        return skuDatas
    }

    fun determineUom(productGroup: String, species: String): Triple<String, String, String> {
        if(FABRICATED_GROUPS.contains(productGroup)) {
            return if (species == NATURAL_TEAK || species == PLANTED_TEAK) {
                Triple("in", "in", "ft")
            } else {
                Triple("in", "in", "m")
            }
        }

        val firstLetter = productGroup[0]
        if(listOf('L', 'D').contains(firstLetter)) {
            return Triple("mm", "mm", "mm")
        }

        if(productGroup == GROUP_CODE_PLYWOOD) {
            return Triple("mm", "mm", "mm")
        }
        return Triple("NA", "NA", "NA")
    }

    fun calculateProcessedWoodVolumn(thickness: BigDecimal, thicknessUom: String, width: BigDecimal, widthUom: String,
                                     length: BigDecimal, lengthUom: String): Pair<BigDecimal, BigDecimal> {
        val volumnFt3Factor: BigDecimal = when {
            thicknessUom == "in" && widthUom == "in" && lengthUom == "ft" -> 0.006944444.toBigDecimal()      // 1/144
            thicknessUom == "in" && widthUom == "in" && lengthUom == "m" -> 0.0228.toBigDecimal()
            thicknessUom == "mm" && widthUom == "mm" && lengthUom == "mm" -> 0.0000000353147.toBigDecimal() // 35.3147/10^9
            else -> BigDecimal.ZERO
        }
        val volumnFt3: BigDecimal = BigDecimal(width.toDouble()) * thickness * length * volumnFt3Factor
        val volumnM3: BigDecimal = volumnFt3.divide(M3_TO_FT3)
        return Pair(volumnFt3, volumnM3)
    }

    fun calculateLogVolumn(circumference: BigDecimal, circumferenceUom: String, length: BigDecimal, lengthUom: String): Pair<BigDecimal, BigDecimal> {
        val volumnM3Factor: BigDecimal = when {
            circumferenceUom == "cm" && lengthUom == "m" -> 0.01.toBigDecimal()
            else -> BigDecimal.ZERO
        }
        val volumnM3: BigDecimal = (circumference.pow(2) * length * volumnM3Factor).divide(BigDecimal(4 * Math.PI))
        val volumnFt3: BigDecimal = volumnM3 * M3_TO_FT3
        return Pair(volumnFt3, volumnM3)
    }
}