package com.champaca.inventorydata.log.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.log.LogService
import com.champaca.inventorydata.log.request.CreatePickedLogFileRequest
import com.champaca.inventorydata.utils.DateTimeUtil
import io.github.evanrupert.excelkt.workbook
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.sql.DataSource

@Service
class CreatePickedLogFileUseCase(
    val dataSource: DataSource,
    val logService: LogService,
    val dateTimeUtil: DateTimeUtil,
) {

    @Value("\${champaca.reports.location}")
    lateinit var reportPath: String

    val logger = LoggerFactory.getLogger(CreatePickedLogFileUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: CreatePickedLogFileRequest): File {
        var pairs: List<Pair<String, Int>> = listOf()

        Database.connect(dataSource)
        transaction {
            addLogger(exposedLogger)
            pairs = logService.getLogsByRefCodes(request.refCodes)
        }

        val dirName = "${reportPath}/inventory/picked"
        Files.createDirectories(Paths.get(dirName))
        val fileName = "${dirName}/Picked_${dateTimeUtil.getCurrentDateTimeString("yyyy_MM_dd_HH_mm")}.xlsx"
        workbook {
            sheet {
                row {
                    cell("MAT Code")
                    cell("Qty")
                    cell("Remark")
                }
                for (pair in pairs) {
                    row {
                        cell(pair.first)
                        cell(pair.second)
                    }
                }
            }
        }.write(fileName)
        return File(fileName)
    }
}