package com.champaca.inventorydata.log.excel.impl

import com.champaca.inventorydata.log.excel.LogFileDataPosition
import org.springframework.stereotype.Component

/**
 * สวนป่าปากปาด
 */
@Component
class PakPatDataPosition: LogFileDataPosition {
    override val orderColumn: Char
        get() = 'A'
    override val speciesColumn: Char
        get() = 'B'
    override val lengthColumn: Char
        get() = 'E'
    override val circumferenceColumn: Char
        get() = 'G'
    override val logNoColumn: Char
        get() = 'J'
    override val volumnM3Column: Char
        get() = 'H'
    override val barcodeColumn: Char
        get() = 'K'
    override val startRow: Int
        get() = 6
    override val lastColumn: Char
        get() = 'K'
}