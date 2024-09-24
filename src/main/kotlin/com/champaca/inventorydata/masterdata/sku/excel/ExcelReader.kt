package com.champaca.inventorydata.masterdata.sku.excel

import com.champaca.inventorydata.common.ChampacaConstant.M3_TO_FT3
import com.champaca.inventorydata.masterdata.sku.model.SkuData
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.math.BigDecimal

@Service
class ExcelReader {
    companion object {
        const val MAT_CODE = 0
        const val ITEM_NAME = 1
        const val MAIN_GROUP = 2
        const val GROUP = 4
        const val GROUP_NAME = 5
        const val WOOD_TYPE = 6
        const val CERTIFICATION = 8
        const val GRADE = 10
        const val THICK = 11
        const val THICKNESS_UOM = 12
        const val WIDTH = 13
        const val WIDTH_UOM = 14
        const val LENGTH = 15
        const val LENGTH_UOM = 16
        const val VOLUMN_FT3 = 17
        const val VOLUMN_M3 = 18
        const val AREA_M2 = 19
        const val CIRCUMFERENCE = 20
        const val CIRCUMFERENCE_UOM = 21
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
            val matCode = row.getCell(MAT_CODE)?.stringCellValue?.trim()
            if (matCode.isNullOrEmpty()) {
                continue
            }
            val mainGroup = row.getCell(MAIN_GROUP).numericCellValue.toInt()
            val group = row.getCell(GROUP).stringCellValue
            val productGroup = "$mainGroup$group"
            val species = row.getCell(WOOD_TYPE).stringCellValue
            val fsc = if(row.getCell(CERTIFICATION).numericCellValue.toInt() == 1) "Y" else "N"
            val name = row.getCell(ITEM_NAME).stringCellValue
            val grade = row.getCell(GRADE)?.stringCellValue ?: ""
            val thickness = row.getCell(THICK).numericCellValue.toBigDecimal()
            val thicknessUom = getUom(row.getCell(THICKNESS_UOM).stringCellValue)
            val width = row.getCell(WIDTH).numericCellValue.toBigDecimal()
            val widthUom = getUom(row.getCell(WIDTH_UOM).stringCellValue)
            val length = row.getCell(LENGTH).numericCellValue.toBigDecimal()
            val lengthUom = getUom(row.getCell(LENGTH_UOM).stringCellValue)
            val volumnFt3 = row.getCell(VOLUMN_FT3).numericCellValue.toBigDecimal()
            val volumnM3 = row.getCell(VOLUMN_M3).numericCellValue.toBigDecimal()
            val areaM2 = if(row.getCell(AREA_M2) != null) row.getCell(AREA_M2).numericCellValue.toBigDecimal() else BigDecimal.ZERO
            val circumference = if(row.getCell(CIRCUMFERENCE) != null) row.getCell(CIRCUMFERENCE).numericCellValue.toBigDecimal() else BigDecimal.ZERO
            val circumferenceUom = if(row.getCell(CIRCUMFERENCE_UOM) != null) getUom(row.getCell(CIRCUMFERENCE_UOM).stringCellValue) else ""

            val skuGroupId = if(skuGroupIds.containsKey(productGroup)) skuGroupIds[productGroup]!! else -1

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
                    areaM2 = areaM2,
                    species = species,
                    grade = grade,
                    fsc = fsc
                )
            )
        }

        return skuDatas.associateBy { it.matCode }.values.toList()
    }

    private fun getUom(uom: String): String {
        return when(uom) {
            "เมตร", "m" -> "m"
            "ฟุต", "ft" -> "ft"
            "มิล", "มิลลิเมตร", "mm" -> "mm"
            "นิ้ว", "in" -> "in"
            "cm" -> "cm"
            else -> ""
        }
    }
}