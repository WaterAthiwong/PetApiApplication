package com.champaca.inventorydata.masterdata.sku.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.dao.ConfigDao
import com.champaca.inventorydata.masterdata.config.ConfigRepository
import com.champaca.inventorydata.masterdata.model.SimpleData
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetFlooringPropertyUseCase(
    val dataSource: DataSource,
    val configRepository: ConfigRepository
) {

    companion object {
        const val COMMA = ","
        const val COLON = ":"
        val PROPERTIES = mapOf(
            "color" to "Sku.Floor.Color",
            "coating" to "Sku.Floor.Coating",
            "pattern" to "Sku.Floor.Pattern",
            "texture" to "Sku.Floor.Texture"
        )
    }

    val logger = LoggerFactory.getLogger(GetFlooringPropertyUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(property: String): List<SimpleData> {
        Database.connect(dataSource)

        var results = listOf<SimpleData>()
        transaction {
            addLogger(exposedLogger)

            val configName = PROPERTIES[property] ?: return@transaction
            val config = configRepository.get(configName)!!
            results = convertConfigValueToSimpleData(config)
        }
        return results
    }

    private fun convertConfigValueToSimpleData(config: ConfigDao): List<SimpleData> {
        val entries = config.valueString!!.split(COMMA)
        return entries.map { entry ->
            val parts = entry.trim().split(COLON)
            SimpleData(id = null, code = parts[1], name = parts[0])
        }
    }
}