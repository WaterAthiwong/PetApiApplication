package com.champaca.inventorydata.log.excel

import com.champaca.inventorydata.model.Species
import java.math.BigDecimal

data class LogFileRow(
    val tabName: String,
    val order: Int,
    val species: Species,
    val length: Int,
    val circumference: Int,
    val logNo: String,
    val quantity: Int,
    val volumnM3: BigDecimal,
    val refCode: String,
) {
    fun getMatCode(fsc: Boolean): String {
        val fsc = if (fsc) 1 else 2
        val species = Species.PT
        val prefix = "1M0${species}${fsc}"
        var lengthStr = "%.2f".format(this.length.toDouble()/100)
        if (lengthStr.endsWith(".00")) {
            // This is for the case like 1M0PT2-70X2.00 which actually needs to be 1M0PT2-70X2
            lengthStr = lengthStr.substring(0, lengthStr.length - 3)
        }
        return "${prefix}-${this.circumference}X${lengthStr}"
    }
}
