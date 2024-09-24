package com.champaca.inventorydata.goodmovement.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.Config
import com.champaca.inventorydata.databasetable.dao.ConfigDao
import com.champaca.inventorydata.goodmovement.response.GetExtraAttributesResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetExtraAttributesUseCase(
    val dataSource: DataSource
) {
    companion object {
        const val CONFIG_NAME = "goodMovement.extraAttributes."
    }

    val logger = LoggerFactory.getLogger(GetExtraAttributesUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(departmentId: Int): GetExtraAttributesResponse {
        var results: List<GetExtraAttributesResponse.ExtraAttributes> = listOf()

        Database.connect(dataSource)
        transaction {
            addLogger(exposedLogger)
            results = getExtraAttributes(departmentId)
        }
        return GetExtraAttributesResponse(results)
    }

    private fun getExtraAttributes(departmentId: Int): List<GetExtraAttributesResponse.ExtraAttributes> {
        val configName = CONFIG_NAME + departmentId
        val config = ConfigDao.find { Config.name eq configName }.firstOrNull()

        return if (config != null) {
            Json.decodeFromString(config.valueString!!)
        } else {
            listOf()
        }
    }

}