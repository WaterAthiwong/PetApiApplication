package com.champaca.inventorydata.pile.request

import com.champaca.inventorydata.databasetable.dao.PileDao
import java.time.LocalDate

data class GetPileByDateLine(
    val productionDateFrom: LocalDate?,
    val productionDateTo: LocalDate?,
    val manufacturingLineId: Int?,
    val supplierId: Int?,
    val pileCodes: List<String>?,
    val departmentId: Int?,
    val createdDateFrom: LocalDate?,
    val createdDateTo: LocalDate?,
    val type: List<String>? = listOf(PileDao.WOOD_PILE),
)