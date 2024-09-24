package com.champaca.inventorydata.cleansing.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.common.ChampacaConstant.M3_TO_FT3
import com.champaca.inventorydata.databasetable.GmItem
import com.champaca.inventorydata.databasetable.Sku
import com.champaca.inventorydata.databasetable.StoreLocationHasLotNo
import com.champaca.inventorydata.databasetable.dao.SkuDao
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class MassConvertToMillimeterUseCase(
    val dataSource: DataSource
) {
    val max = 900000
    val fetch = 300000
//    val max = 300
//    val fetch = 100
    val cube = 1000000000.toBigDecimal()

    val inchConversion = mapOf(
        0.125.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 3.toBigDecimal(),
        0.13.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 3.toBigDecimal(),
        0.25.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 6.toBigDecimal(),
        0.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 13.toBigDecimal(),
        0.625.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 16.toBigDecimal(),
        0.63.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 16.toBigDecimal(),
        0.75.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 19.toBigDecimal(),
        0.875.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 22.toBigDecimal(),
        0.88.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 22.toBigDecimal(),
        1.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 25.toBigDecimal(),
        1.125.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 29.toBigDecimal(),
        1.13.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 29.toBigDecimal(),
        1.25.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 32.toBigDecimal(),
        1.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 38.toBigDecimal(),
        1.625.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 41.toBigDecimal(),
        1.63.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 41.toBigDecimal(),
        1.75.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 44.toBigDecimal(),
        1.88.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 48.toBigDecimal(),
        2.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 51.toBigDecimal(),
        2.13.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 54.toBigDecimal(),
        2.25.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 57.toBigDecimal(),
        2.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 64.toBigDecimal(),
        2.63.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 67.toBigDecimal(),
        2.75.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 70.toBigDecimal(),
        2.88.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 73.toBigDecimal(),
        3.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 76.toBigDecimal(),
        3.25.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 83.toBigDecimal(),
        3.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 89.toBigDecimal(),
        3.75.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 95.toBigDecimal(),
        4.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 102.toBigDecimal(),
        4.25.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 108.toBigDecimal(),
        4.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 114.toBigDecimal(),
        4.75.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 121.toBigDecimal(),
        5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 127.toBigDecimal(),
        5.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 140.toBigDecimal(),
        5.75.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 146.toBigDecimal(),
        6.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 152.toBigDecimal(),
        6.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 165.toBigDecimal(),
        6.75.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 171.toBigDecimal(),
        7.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 178.toBigDecimal(),
        7.75.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 197.toBigDecimal(),
        8.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 203.toBigDecimal(),
        9.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 229.toBigDecimal(),
        10.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 254.toBigDecimal(),
        10.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 267.toBigDecimal(),
        11.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 279.toBigDecimal(),
        12.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 305.toBigDecimal(),
        3.63.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 92.toBigDecimal(),
        5.25.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 133.toBigDecimal(),
        6.25.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 159.toBigDecimal(),
        7.25.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 184.toBigDecimal(),
        7.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 191.toBigDecimal(),
        8.25.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 210.toBigDecimal(),
        8.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 216.toBigDecimal(),
        9.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 241.toBigDecimal(),
        9.75.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 248.toBigDecimal(),
        11.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 292.toBigDecimal(),
        11.75.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 298.toBigDecimal(),
        12.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 305.toBigDecimal(),
        13.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 330.toBigDecimal(),
        14.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 356.toBigDecimal(),
        15.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 381.toBigDecimal(),
        16.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 406.toBigDecimal(),
        17.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 432.toBigDecimal(),
        18.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 457.toBigDecimal(),
        19.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 483.toBigDecimal(),
        20.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 508.toBigDecimal(),
        22.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 559.toBigDecimal(),
    )

    val footConversion = mapOf(
        0.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 150.toBigDecimal(),
        1.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 300.toBigDecimal(),
        1.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 450.toBigDecimal(),
        2.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 600.toBigDecimal(),
        2.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 750.toBigDecimal(),
        3.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 900.toBigDecimal(),
        3.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 1050.toBigDecimal(),
        4.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 1200.toBigDecimal(),
        4.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 1350.toBigDecimal(),
        5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 1500.toBigDecimal(),
        5.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 1650.toBigDecimal(),
        6.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 1800.toBigDecimal(),
        6.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 1950.toBigDecimal(),
        7.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 2100.toBigDecimal(),
        7.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 2250.toBigDecimal(),
        8.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 2400.toBigDecimal(),
        8.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 2550.toBigDecimal(),
        9.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 2700.toBigDecimal(),
        9.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 2850.toBigDecimal(),
        10.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 3000.toBigDecimal(),
        10.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 3150.toBigDecimal(),
        11.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 3300.toBigDecimal(),
        11.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 3450.toBigDecimal(),
        12.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 3600.toBigDecimal(),
        12.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 3750.toBigDecimal(),
        13.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 3900.toBigDecimal(),
        13.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 4050.toBigDecimal(),
        14.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 4200.toBigDecimal(),
        14.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 4350.toBigDecimal(),
        15.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 4500.toBigDecimal(),
        15.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 4650.toBigDecimal(),
        16.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 4800.toBigDecimal(),
        16.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 4950.toBigDecimal(),
        17.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 5100.toBigDecimal(),
        17.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 5250.toBigDecimal(),
        18.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 5400.toBigDecimal(),
        18.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 5550.toBigDecimal(),
        19.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 5700.toBigDecimal(),
        19.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 5850.toBigDecimal(),
        20.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 6000.toBigDecimal(),
        20.5.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 6150.toBigDecimal(),
        21.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 6300.toBigDecimal(),
        22.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 6600.toBigDecimal(),
        23.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 6900.toBigDecimal(),
        24.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 7200.toBigDecimal(),
        25.toBigDecimal().setScale(5, RoundingMode.HALF_UP) to 7500.toBigDecimal(),
    )

    fun execute() {
        Database.connect(dataSource)
        val trouble = mutableListOf<String>()
//        for (i in 0..max step fetch) {
            transaction {
                val now = LocalDateTime.now()
                addLogger(ExposedInfoLogger)
//                val skus = getSku(i)
//                skus.forEach { sku ->
//                    if (sku.thicknessUom == "in" && sku.widthUom == "in" && sku.lengthUom in listOf("m", "ft")) {
//                        val width = inchConversion[sku.width]
//                        if (width == null) {
//                            trouble.add(sku.matCode)
//                            return@forEach
//                        }
//                        val thickness = inchConversion[sku.thickness]
//                        if (thickness == null) {
//                            trouble.add(sku.matCode)
//                            return@forEach
//                        }
//                        val length = when(sku.lengthUom) {
//                            "m" -> sku.length.multiply(1000.toBigDecimal())
//                            "ft" -> footConversion[sku.length]
//                            else -> null
//                        }
//                        if (length == null) {
//                            trouble.add(sku.matCode)
//                            return@forEach
//                        }
//                        val volumnM3 = (width * thickness * length).divide(cube)
//                        val volumnFt3 = volumnM3.multiply(M3_TO_FT3)
//                        val matCode = sku.matCode.substringBefore("-") + "-${thickness.setScale(0, RoundingMode.HALF_UP)}X${width.setScale(0, RoundingMode.HALF_UP)}X${length.setScale(0, RoundingMode.HALF_UP)}"
//                        val name = sku.name.substringBeforeLast("-") + "-${thickness.setScale(0, RoundingMode.HALF_UP)}X${width.setScale(0, RoundingMode.HALF_UP)}X${length.setScale(0, RoundingMode.HALF_UP)}"
//                        Sku.update({ Sku.id eq sku.id }) {
//                            it[Sku.width] = width
//                            it[Sku.widthUom] = "mm"
//                            it[Sku.thickness] = thickness
//                            it[Sku.thicknessUom] = "mm"
//                            it[Sku.length] = length
//                            it[Sku.lengthUom] = "mm"
//                            it[Sku.volumnM3] = volumnM3
//                            it[Sku.volumnFt3] = volumnFt3
//                            it[Sku.matCode] = matCode
//                            it[Sku.name] = name
//                            it[Sku.updatedAt] = now
//                        }
//                    } else {
//                        trouble.add(sku.matCode)
//                    }
//                }
//            }
        }
        println("Trouble: $trouble")
    }

    private fun getSku(offset: Int): List<SkuDao> {
        val today = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0)
        val query = SkuDao.find { (Sku.status eq "D") and (Sku.species inList listOf("PT")) and (Sku.widthUom neq "mm") and (Sku.thicknessUom neq "mm") and (Sku.lengthUom neq "mm") and (Sku.skuGroupId notInList listOf(1, 114)) }
        .limit(fetch, offset.toLong())
        return query.toList()
    }
}