package com.champaca.inventorydata.masterdata.manufacturingline.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.ManufacturingLine
import com.champaca.inventorydata.databasetable.ProcessType
import com.champaca.inventorydata.masterdata.manufacturingline.model.ManufacturingLineData
import com.champaca.inventorydata.masterdata.manufacturingline.request.GetManufacturingLineRequest
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetManufacturingLineUseCase(
    val dataSource: DataSource
) {
    val logger = LoggerFactory.getLogger(GetManufacturingLineUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(sessionId: String, request: GetManufacturingLineRequest): List<ManufacturingLineData> {
        Database.connect(dataSource)
        var results = listOf<ManufacturingLineData>()
        transaction {
            addLogger(exposedLogger)
            val processTypeIds = request.processTypeIds ?: listOf(request.processTypeId!!)
            results = getManufacturingLines(processTypeIds)
        }
        return results
    }

    private fun getManufacturingLines(processTypeIds: List<Int>): List<ManufacturingLineData> {
        val joins = ManufacturingLine.join(ProcessType, JoinType.INNER) { ManufacturingLine.processTypeId eq ProcessType.id }
        val query = joins.select(ManufacturingLine.id, ManufacturingLine.processTypeId, ManufacturingLine.name, ProcessType.name, ProcessType.prefix)
            .where { (ManufacturingLine.status eq "A") and (ProcessType.status eq "A") and (ProcessType.id inList processTypeIds) }
        return query.map { resultRow ->
            ManufacturingLineData(
                id = resultRow[ManufacturingLine.id].value,
                processTypeId = resultRow[ManufacturingLine.processTypeId],
                name = resultRow[ManufacturingLine.name],
                processName = resultRow[ProcessType.name],
                prefix = resultRow[ProcessType.prefix]
            )
        }
    }
}