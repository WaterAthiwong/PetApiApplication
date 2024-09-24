package com.champaca.inventorydata.pallet.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.databasetable.Pallet
import com.champaca.inventorydata.masterdata.config.ConfigRepository
import com.champaca.inventorydata.utils.DateTimeUtil
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class CreatePalletUseCase(
    val dataSource: DataSource,
    val configRepository: ConfigRepository,
    val dateTimeUtil: DateTimeUtil
) {
    fun execute(processPrefix: String): String {
        Database.connect(dataSource)

        var palletCode = ""
        transaction {
            addLogger(ExposedInfoLogger)

            val prefix = getPrefix(processPrefix)
            val config = configRepository.getOrPut("Pallet.$prefix", defaultInt = 0)
            palletCode = "P${prefix}${(config.valueInt!! + 1).toString().padStart(4, '0')}"
            config.valueInt = config.valueInt!! + 1

            Pallet.insert {
                val now = LocalDateTime.now()
                it[this.code] = palletCode
                it[this.createdAt] = now
                it[this.updatedAt] = now
                it[this.status] = "A"
            }
        }

        return palletCode
    }

    private fun getPrefix(processPrefix: String): String {
        val date = LocalDate.now()
        return "$processPrefix${dateTimeUtil.getYearMonthPrefix()}"
    }
}