package com.champaca.inventorydata.masterdata.department.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.ManufacturingLine
import com.champaca.inventorydata.databasetable.ProcessType
import com.champaca.inventorydata.masterdata.manufacturingline.ManufacturingLineRepository
import com.champaca.inventorydata.masterdata.model.SimpleData
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetProcessAndManufacturingLineUseCase(
    val dataSource: DataSource,
    val manufacturingLineRepository: ManufacturingLineRepository
) {
    val logger = LoggerFactory.getLogger(GetProcessAndManufacturingLineUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(departmentId: Int): List<SimpleData> {
        Database.connect(dataSource)

        var results = listOf<SimpleData>()
        transaction {
            addLogger(exposedLogger)
            val rows = manufacturingLineRepository.getProcessAndManufacturingLine(departmentId)
            results = toSimpleData(rows)
        }

        return results
    }

    private fun toSimpleData(rows: List<ResultRow>): List<SimpleData> {
        val processGroup = rows.groupBy { it[ProcessType.id].value }
        return processGroup.map { (processId, processRows) ->
            val processRow = processRows.first()
            val processData = SimpleData(
                id = processId,
                name = processRow[ProcessType.name],
                code = processRow[ProcessType.prefix],
                data = processRows.map { row ->
                    SimpleData(
                        id = row[ManufacturingLine.id].value,
                        name = row[ManufacturingLine.name]
                    )
                }
            )
            processData
        }
    }
}