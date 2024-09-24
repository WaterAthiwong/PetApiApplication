package com.champaca.inventorydata.masterdata.department.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.dao.DepartmentDao
import com.champaca.inventorydata.masterdata.model.SimpleData
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetDepartmentUseCase(
    val dataSource: DataSource
) {
    val logger = LoggerFactory.getLogger(GetDepartmentUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(): List<SimpleData> {
        Database.connect(dataSource)

        var results = listOf<SimpleData>()
        transaction {
            addLogger(exposedLogger)

            results = getDepartments()
        }

        return results
    }

    private fun getDepartments(): List<SimpleData> {
        return DepartmentDao.all().map {
            SimpleData(id = it.id.value, name = it.name)
        }
    }
}