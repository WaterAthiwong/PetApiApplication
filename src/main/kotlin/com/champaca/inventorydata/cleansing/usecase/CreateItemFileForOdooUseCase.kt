package com.champaca.inventorydata.cleansing.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.databasetable.Sku
import com.champaca.inventorydata.databasetable.SkuGroup
import com.champaca.inventorydata.databasetable.dao.SkuDao
import io.github.evanrupert.excelkt.workbook
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths
import javax.sql.DataSource

@Service
class CreateItemFileForOdooUseCase(
    val dataSource: DataSource
) {
    @Value("\${champaca.reports.location}")
    lateinit var reportPath: String

    val TOTAL = 1200000
    val PER_FILE = 100000

    val groupToSub = mapOf(
        1 to "สินค้าที่จัดเก็บได้ / RM - LOG / RM-Log",
        2 to "สินค้าที่จัดเก็บได้ / RM - SAWN TIMBER / RM-Sawn Timber",
        6 to "สินค้าที่จัดเก็บได้ / RM - SAWN TIMBER / RM-Sawn Timber KD",
        5 to "สินค้าที่จัดเก็บได้ / RM - SAWN TIMBER / RM-Sawn Timber KD ACQ",
        3 to "สินค้าที่จัดเก็บได้ / WIP - OTHER / RM-Slab",
        7 to "สินค้าที่จัดเก็บได้ / WIP - OTHER / RM-Slab KD",
        4 to "สินค้าที่จัดเก็บได้ / RM - SAWN TIMBER / RM-Sawn Timber ACQ",
        9 to "สินค้าที่จัดเก็บได้ / RM - LAMINATED BLOCK / RM-Laminated Block",
        8 to "สินค้าที่จัดเก็บได้ / RM - PLYWOOD / RM-Plywood",
        10 to "สินค้าที่จัดเก็บได้ / SEMI - LAMELLA / Component-Lamella",
        72 to "สินค้าที่จัดเก็บได้ / SEMI - BASE (CORE+BALANCER) / Component-Base1",
        73 to "สินค้าที่จัดเก็บได้ / SEMI - BASE (CORE+BALANCER) / Component-Base2",
        74 to "สินค้าที่จัดเก็บได้ / SEMI - BASE (CORE+BALANCER) / Component-Base3",
        67 to "สินค้าที่จัดเก็บได้ / SEMI - HFG / HFG-Compound Floor",
        68 to "สินค้าที่จัดเก็บได้ / SEMI - HFG / HFG-Engineer Floor",
        70 to "สินค้าที่จัดเก็บได้ / SEMI - HFG / HFG-Solid Floor",
        119 to "สินค้าที่จัดเก็บได้ / SEMI - TFG / TFG-Compound Floor",
        115 to "สินค้าที่จัดเก็บได้ / SEMI - TFG / TFG-Engineer Floor",
        117 to "สินค้าที่จัดเก็บได้ / SEMI - TFG / TFG-Solid Floor",
        110 to "สินค้าที่จัดเก็บได้ / RM - FINGER JOINT / RM-Finger Joint",
        )

    fun execute() {
        Database.connect(dataSource)

        transaction {
            addLogger(ExposedInfoLogger)
            for (i in 0 until TOTAL step PER_FILE) {
                val items = getItems(i)
                createFile(i, items)
            }
        }
    }

    private fun getItems(offset: Int): List<SkuDao> {
        return SkuDao.find { (Sku.status eq "A") and (Sku.skuGroupId inList listOf(1, 2, 6, 5, 3, 7, 4, 9, 8, 10, 72, 73, 74, 67, 68, 70, 119, 115, 117,110)) }
            .limit(PER_FILE, offset.toLong())
            .toList()
    }

    private fun createFile(count: Int, items: List<SkuDao>) {
        val dirName = reportPath
        Files.createDirectories(Paths.get(dirName))
        val fileName = "${dirName}/Item_${count + 1}.xlsx"

        workbook {
            sheet {
                row {
                    cell("External ID")
                    cell("Name")
                    cell("Can be Sold")
                    cell("Can be Purchased")
                    cell("Can be Expensed")
                    cell("Product Type")
                    cell("Product Category")
                    cell("Internal Reference")
                    cell("Customer Taxes")
                    cell("Cost")
                    cell("Unit of Measure")
                    cell("Purchase Unit of Measure")
                    cell("Invoicing Policy")
                    cell("Vendor Taxes")
                    cell("Control Policy")
                    cell("Routes")
                    cell("Tracking")
                    cell("Production Location")
                    cell("Inventory Location")
                    cell("Cubic Meter Factor (M3)")
                    cell("Square Meter Factor (M2)")
                    cell("Cubic Foot Factor (F3)")
                    cell("Unit Price per M2")
                }

                items.forEach { item ->
                    val productCat = groupToSub[item.skuGroupId]
                    if (productCat != null) {
                        row {
                            cell("")
                            cell(item.name)
                            cell("FALSE")
                            cell("TRUE")
                            cell("FALSE")
                            cell("Storable Product")
                            cell(productCat) // TODO
                            cell(item.matCode)
                            cell("ภาษีขาย 7%")
                            cell("")
                            cell(if (item.skuGroupId == 1) "M3" else "PCS")
                            cell(if (item.skuGroupId == 1) "M3" else "PCS")
                            cell("Ordered quantities")
                            cell("ภาษีซื้อ 7%")
                            cell("On ordered quantities")
                            cell("Buy")
                            cell(if (item.skuGroupId == 1) "By Lots" else "No Tracking")
                            cell("Virtual Locations/Production")
                            cell("Virtual Locations/Inventory adjustment")
                            cell(item.volumnM3.setScale(5, RoundingMode.HALF_UP))
                            cell(item.areaM2?.setScale(5, RoundingMode.HALF_UP) ?: "")
                            cell(item.volumnFt3)
                            cell("")
                        }
                    }
                }
            }
        }.write(fileName)
    }
}