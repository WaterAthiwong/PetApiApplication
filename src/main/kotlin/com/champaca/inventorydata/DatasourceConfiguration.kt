package com.champaca.inventorydata

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DatasourceConfiguration {

    @Value("\${wms.datasource.driverClassName}")
    lateinit var dbDriverClassName: String

    @Value("\${wms.datasource.url}")
    lateinit var dbUrl: String

    @Value("\${wms.datasource.username}")
    lateinit var dbUsername: String

    @Value("\${wms.datasource.password}")
    lateinit var dbPassword: String

    @Value("\${wms.datasource.hikari.maxPoolSize}")
    var maxPoolSize: Int = 0

    @Value("\${wms.datasource.hikari.maxLifetime}")
    var maxLifetime: Int = 0

    @Bean
    fun getDatasource(): DataSource {
        val config = HikariConfig().apply {
            jdbcUrl         = dbUrl
            driverClassName = dbDriverClassName
            username        = dbUsername
            password        = dbPassword
            maximumPoolSize = maxPoolSize
            maxLifetime     = maxLifetime
            addDataSourceProperty("rewriteBatchedStatements", true)
        }
        return HikariDataSource(config)
    }
}