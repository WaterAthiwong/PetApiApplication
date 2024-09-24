package com.champaca.inventorydata.pile.request

import com.champaca.inventorydata.databasetable.dao.PileDao

data class CreatePileRequest(
    val processPrefix: String,
    val goodMovementId: Int,
    val manufacturingLineId: Int?,
    val orderNo: String?,
    val items: List<PileItem> = emptyList(),
    val location: String,
    val remark: String,
    val refNo: String?,
    val customer: String?,

    // the value should be either PileDao.WOOD_PILE or PileDao.FG_BOX. This is to help tracking the pile easier.
    val pileType: String = PileDao.WOOD_PILE,
    val returned: Boolean = false,
    val returnedJobNo: String?,
    val copies: Int = 1
) {
}