package com.champaca.inventorydata.pile.request

import com.champaca.inventorydata.databasetable.dao.PileDao

data class PrintPileRequest (
    val pileCodes: List<String>,
    val preferredMeasurement: String = "Ft3",
    val format: String = PileDao.WOOD_PILE,
)