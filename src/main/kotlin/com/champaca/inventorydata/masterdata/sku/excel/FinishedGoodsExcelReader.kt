package com.champaca.inventorydata.masterdata.sku.excel

import com.champaca.inventorydata.common.ChampacaConstant.M3_TO_FT3
import com.champaca.inventorydata.masterdata.sku.model.SkuData
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.math.BigDecimal

@Service
class FinishedGoodsExcelReader {
    companion object {
        const val MAT_CODE = 0
        const val ITEM_NAME = 1
        const val GROUP = 2
        const val GROUP_NAME = 3
        const val WOOD_TYPE = 4
        const val CERTIFICATION = 6
        const val GRADE = 8
        const val THICK = 9
        const val WIDTH = 10
        const val LENGTH = 11
        const val PATTERN_NAME = 12
        const val PATTERN_CODE = 13
        const val TEXTURE_NAME = 14
        const val TEXTURE_CODE = 15
        const val COATING_NAME = 16
        const val COATING_CODE = 17
        const val COLOR_NAME = 18
        const val COLOR_CODE = 19
        const val VOLUMN_FT3 = 20
        const val VOLUMN_M3 = 21
        const val AREA_M2 = 22
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
            var matCode = row.getCell(MAT_CODE)?.stringCellValue?.trim()
            if (matCode.isNullOrEmpty()) {
                continue
            }
            val group = row.getCell(GROUP).stringCellValue
            val productGroup = "5$group"
            val species = row.getCell(WOOD_TYPE).stringCellValue
            val fsc = if(row.getCell(CERTIFICATION).numericCellValue.toInt() == 1) "Y" else "N"
            var name = row.getCell(ITEM_NAME).stringCellValue
            val grade = row.getCell(GRADE)?.stringCellValue ?: ""
            val thickness = row.getCell(THICK).numericCellValue.toBigDecimal()
            val width = row.getCell(WIDTH).numericCellValue.toBigDecimal()
            val length = row.getCell(LENGTH).numericCellValue.toBigDecimal()
            val volumnFt3 = row.getCell(VOLUMN_FT3).numericCellValue.toBigDecimal()
            val volumnM3 = row.getCell(VOLUMN_M3).numericCellValue.toBigDecimal()
            val areaM2 = if(row.getCell(AREA_M2) != null) row.getCell(AREA_M2).numericCellValue.toBigDecimal() else BigDecimal.ZERO
            val patternName = row.getCell(PATTERN_NAME).stringCellValue
            val patternCode = row.getCell(PATTERN_CODE).stringCellValue
            val textureName = row.getCell(TEXTURE_NAME).stringCellValue
            val textureCode = row.getCell(TEXTURE_CODE).stringCellValue
            val coatingName = row.getCell(COATING_NAME).stringCellValue
            val coatingCode = row.getCell(COATING_CODE).stringCellValue
            val colorName = row.getCell(COLOR_NAME).stringCellValue
            val colorCode = row.getCell(COLOR_CODE).stringCellValue
            matCode = "$matCode-$patternCode$textureCode-$coatingCode$colorCode"
            name = "$name-$patternName-$textureName-$coatingName-$colorName"

            val skuGroupId = if(skuGroupIds.containsKey(productGroup)) skuGroupIds[productGroup]!! else -1

            skuDatas.add(
                SkuData(
                    skuGroupId = skuGroupId,
                    skuGroupName = productGroup,
                    code = "SKU000000001",
                    matCode = matCode,
                    name = name,
                    thickness = thickness,
                    thicknessUom = "mm",
                    width = width,
                    widthUom = "mm",
                    length = length,
                    lengthUom = "mm",
                    circumference = BigDecimal.ZERO,
                    circumferenceUom = "mm",
                    volumnFt3 = volumnFt3,
                    volumnM3 = volumnM3,
                    areaM2 = areaM2,
                    species = species,
                    grade = grade,
                    fsc = fsc,
                    extraAttributes = mapOf(
                        "pattern" to patternName,
                        "texture" to textureName,
                        "coating" to coatingName,
                        "color" to colorName,
                    )
                )
            )
        }

        return skuDatas.associateBy { it.matCode }.values.toList()
    }
}