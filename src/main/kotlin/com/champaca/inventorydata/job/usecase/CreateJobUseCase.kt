package com.champaca.inventorydata.job.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.Job
import com.champaca.inventorydata.job.request.CreateJobRequest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class CreateJobUseCase(
    val dataSource: DataSource
) {
    companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    fun execute(request: CreateJobRequest): ResultOf<String> {
        Database.connect(dataSource)

        transaction {
            addLogger(ExposedInfoLogger)
            val fsc = when(request.fsc) {
                "FSC" -> true
                "NON FSC" -> false
                else -> null
            }
            Job.insert {
                it[this.jobNo] = request.jobNo
                it[this.orderNo] = request.orderNo
                it[this.invoiceNo] = request.invoiceNo
                it[this.lotNo] = request.lotNo
                it[this.fsc] = fsc
                it[this.productionDate] = LocalDate.parse(request.productionDate, DATE_FORMAT)
                it[this.endDate] = if (request.endDate.isNullOrEmpty()) null else LocalDate.parse(request.endDate, DATE_FORMAT)
                it[this.createdAt] = LocalDateTime.now()
            }
        }
        return ResultOf.Success("")
    }
}