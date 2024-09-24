package com.champaca.inventorydata.log.excel.impl

import com.champaca.inventorydata.log.excel.LogFileDataPosition
import org.springframework.stereotype.Component

/**
 * สวนป่าแม่เมาะ
 */
@Component
class MaeMohDataPosition: LogFileDataPosition {
    override val orderColumn: Char
        get() = 'A'
    override val speciesColumn: Char
        get() = 'B'
    override val lengthColumn: Char
        get() = 'E'
    override val circumferenceColumn: Char
        get() = 'F'
    override val logNoColumn: Char
        get() = 'I'
    override val volumnM3Column: Char
        get() = 'G'
    override val barcodeColumn: Char
        get() = 'J'
    override val startRow: Int
        get() = 5
    override val lastColumn: Char
        get() = 'J'
}