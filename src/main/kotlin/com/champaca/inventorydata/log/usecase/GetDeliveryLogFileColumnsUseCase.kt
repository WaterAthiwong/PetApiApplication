package com.champaca.inventorydata.log.usecase

import com.champaca.inventorydata.log.excel.LogFileDataPosition
import org.springframework.stereotype.Service

@Service
class GetDeliveryLogFileColumnsUseCase(
    val logDataPositions: Map<Int, LogFileDataPosition>){

    fun execute(supplierId: Int): LogFileDataPosition? {
        if (logDataPositions.containsKey(supplierId)) {
            val logDataPosition = logDataPositions[supplierId]!!
            return object : LogFileDataPosition {
                override val orderColumn: Char
                    get() = logDataPosition.orderColumn
                override val speciesColumn: Char
                    get() = logDataPosition.speciesColumn
                override val lengthColumn: Char
                    get() = logDataPosition.lengthColumn
                override val circumferenceColumn: Char
                    get() = logDataPosition.circumferenceColumn
                override val logNoColumn: Char
                    get() = logDataPosition.logNoColumn
                override val volumnM3Column: Char
                    get() = logDataPosition.volumnM3Column
                override val barcodeColumn: Char
                    get() = logDataPosition.barcodeColumn
                override val startRow: Int
                    get() = logDataPosition.startRow + 1
                override val lastColumn: Char
                    get() = logDataPosition.lastColumn
            }
        }
        return null
    }
}