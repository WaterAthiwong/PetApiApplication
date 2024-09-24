package com.champaca.inventorydata.masterdata.supplier.excel

import com.champaca.inventorydata.masterdata.supplier.model.SupplierData
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.stereotype.Service
import java.io.FileInputStream

@Service
class SupplierExcelReader {
    companion object {
        const val TYPE = 0
        const val NAME = 1
        const val ERP_CODE = 2
        const val TAXPAYER_NO = 3
        const val ADDRESS = 4
        const val CONTACT = 5
        const val PHONE = 6
        const val EMAIL = 7
        const val REMARK = 8
    }

    fun readFile(filePath: String): List<SupplierData> {
        // Reference: https://chercher.tech/kotlin/read-write-excel-kotlin
        val inputStream = FileInputStream(filePath)
        var xlWb = WorkbookFactory.create(inputStream)
        val xlWs = xlWb.getSheetAt(0)

        // Reference for using physicalNumberOfRows https://www.baeldung.com/java-excel-find-last-row#2-using-getphysicalnumberofrows
        val xlRows = xlWs.physicalNumberOfRows - 1
        var startIndex = 1;
        val supplierData = mutableListOf<SupplierData>()
        for(index in startIndex..xlRows) {
            val row = xlWs.getRow(index)
            val type = row.getCell(TYPE)?.stringCellValue?.trim()
            if (type.isNullOrEmpty()) {
                continue
            }
            val name = row.getCell(NAME).stringCellValue.trim()
            if (name.isEmpty()) {
                continue
            }
            val erpCode = row.getCell(ERP_CODE)?.stringCellValue?.trim()
            val taxpayerNo = row.getCell(TAXPAYER_NO)?.stringCellValue?.trim()
            val address = row.getCell(ADDRESS)?.stringCellValue?.trim()
            val contact = row.getCell(CONTACT)?.stringCellValue?.trim()
            val phone = row.getCell(PHONE)?.stringCellValue?.trim()
            val email = row.getCell(EMAIL)?.stringCellValue?.trim()
            val remark = row.getCell(REMARK)?.stringCellValue?.trim()

            supplierData.add(
                SupplierData(
                    type = getType(type),
                    name = name,
                    erpCode = erpCode,
                    taxNo = taxpayerNo,
                    address = address,
                    contact = contact,
                    phone = phone,
                    email = email,
                    remark = remark
                )
            )
        }

        return supplierData.associateBy { it.name }.values.toList()
    }

    private fun getType(type: String): String {
        return when(type) {
            "Supplier" -> "supplier"
            "สวนป่า" -> "forestry"
            "ลูกค้า" -> "customer"
            "หน่วยงานภายใน" -> "internal"
            "ลูกค้าและ Supplier" -> "supCust"
            else -> ""
        }
    }
}