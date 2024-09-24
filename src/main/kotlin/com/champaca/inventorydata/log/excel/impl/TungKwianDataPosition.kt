package com.champaca.inventorydata.log.excel.impl

import com.champaca.inventorydata.log.excel.LogFileDataPosition
import org.springframework.stereotype.Component

/**
 * สวนป่าทุ่งเกวียน
 */
@Component
class TungKwianDataPosition: LogFileDataPosition {
    override val orderColumn: Char
        get() = 'A'
    override val speciesColumn: Char
        get() = 'B'
    override val lengthColumn: Char
        get() = 'E'
    override val circumferenceColumn: Char
        get() = 'G'
    override val logNoColumn: Char
        get() = 'K'
    override val volumnM3Column: Char
        get() = 'H'
    override val barcodeColumn: Char
        get() = 'L'
    override val startRow: Int
        get() = 5
    override val lastColumn: Char
        get() = 'L'
}