package com.champaca.inventorydata.log.excel.impl

import com.champaca.inventorydata.log.excel.LogFileDataPosition
import org.springframework.stereotype.Component

/**
 * สวนป่าศรีสัชนาลัย
 */
@Component
class SrisatchanalaiDataPosition: LogFileDataPosition {
    override val orderColumn: Char
        get() = 'B'
    override val speciesColumn: Char
        get() = 'C'
    override val lengthColumn: Char
        get() = 'D'
    override val circumferenceColumn: Char
        get() = 'E'
    override val logNoColumn: Char
        get() = 'F'
    override val volumnM3Column: Char
        get() = 'H'
    override val barcodeColumn: Char
        get() = 'J'
    override val startRow: Int
        get() = 2
    override val lastColumn: Char
        get() = 'J'
}