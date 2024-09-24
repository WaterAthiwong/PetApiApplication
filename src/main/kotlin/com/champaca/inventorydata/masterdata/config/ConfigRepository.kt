package com.champaca.inventorydata.masterdata.config

import com.champaca.inventorydata.databasetable.Config
import com.champaca.inventorydata.databasetable.dao.ConfigDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ConfigRepository {

    fun getOrPut(key: String,
                defaultStr: String? = null,
                defaultInt: Int? = null,
                defaultJson: Map<String, String>? = null): ConfigDao {
        val config = ConfigDao.find { Config.name eq key }.firstOrNull()
        return if (config != null) {
            config
        } else {
            ConfigDao.new {
                name = key
                valueInt = defaultInt
                valueString = defaultStr
                createdAt = LocalDateTime.now()
            }
        }
    }

    @Cacheable("configs")
    fun get(key: String): ConfigDao? {
        return ConfigDao.find { Config.name eq key }.firstOrNull()
    }
}