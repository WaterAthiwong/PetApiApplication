package com.champaca.inventorydata.pile.request

import com.champaca.inventorydata.databasetable.dao.PileTransactionDao

data class PickPileRequest(
    val pileCode: String,
    val goodMovementId: Int,
    val manufacturingLineId: Int?,
    val transactionType: String = PileTransactionDao.PICK
)