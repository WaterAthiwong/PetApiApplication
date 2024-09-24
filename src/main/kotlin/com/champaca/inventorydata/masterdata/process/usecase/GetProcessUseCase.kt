package com.champaca.inventorydata.masterdata.process.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.ManufacturingLine
import com.champaca.inventorydata.databasetable.ProcessType
import com.champaca.inventorydata.masterdata.manufacturingline.model.ManufacturingLineData
import com.champaca.inventorydata.masterdata.process.response.GetProcessResponse
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetProcessUseCase(
    val dataSource: DataSource
) {
    val logger = LoggerFactory.getLogger(GetProcessUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun exeucte(): GetProcessResponse {
        Database.connect(dataSource)

        var results = listOf<GetProcessResponse.ProcessData>()
        transaction {
            addLogger(exposedLogger)
            results = getProcesses()
        }

        return GetProcessResponse(results)
    }

    private fun getProcesses(): List<GetProcessResponse.ProcessData> {
        val joins = ProcessType.join(ManufacturingLine, JoinType.INNER) { ProcessType.id eq ManufacturingLine.processTypeId }
        val query = joins.select(ProcessType.id, ProcessType.name, ProcessType.prefix, ManufacturingLine.id, ManufacturingLine.name)
        val rows = query.toList().groupBy({ it[ProcessType.id].value }, { it })
        val results = mutableListOf<GetProcessResponse.ProcessData>()
        rows.forEach { (processId, rows) ->
            val processRow = rows.first()
            val manufacturingLines = rows.map { row ->
                ManufacturingLineData(
                    id = row[ManufacturingLine.id].value,
                    name = row[ManufacturingLine.name],
                    prefix = row[ProcessType.prefix],
                    processTypeId = processId,
                    processName = processRow[ProcessType.name]
                )
            }
            results.add(GetProcessResponse.ProcessData(processRow[ProcessType.id].value, processRow[ProcessType.name], manufacturingLines))
        }
        return results
    }
}