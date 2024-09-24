package com.champaca.inventorydata.log.excel.impl

import com.champaca.inventorydata.log.excel.LogFileDataPosition
import org.springframework.stereotype.Component

/**
 * สวนป่าคลองสวนหมาก - คลองขลุง
 */
@Component
class KlongSuanMakDataPosition: LogFileDataPosition {
    override val orderColumn: Char
        get() = 'A'
    override val speciesColumn: Char
        get() = 'B'
    override val lengthColumn: Char
        get() = 'C'
    override val circumferenceColumn: Char
        get() = 'D'
    override val logNoColumn: Char
        get() = 'F'
    override val volumnM3Column: Char
        get() = 'E'
    override val barcodeColumn: Char
        get() = 'G'
    override val startRow: Int
        get() = 5
    override val lastColumn: Char
        get() = 'G'
}