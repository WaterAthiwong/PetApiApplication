package com.champaca.inventorydata.odoo.usecase

import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.Supplier
import com.champaca.inventorydata.databasetable.dao.SupplierDao.Companion.CUSTOMER
import com.champaca.inventorydata.databasetable.dao.SupplierDao.Companion.SUPCUST
import com.champaca.inventorydata.databasetable.dao.SupplierDao.Companion.SUPPLIER
import com.champaca.inventorydata.odoo.request.UpsertPartnerRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class UpdatePartnerUseCase(
    val dataSource: DataSource
) {
    val logger = LoggerFactory.getLogger(this::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: UpsertPartnerRequest) {
        val payload = Json.encodeToString(request)
        logger.info("Payload: $payload")
        Database.connect(dataSource)
        transaction {
            addLogger(exposedLogger)
            val type = when {
                request.isSupplier && request.isCustomer -> SUPCUST
                request.isSupplier -> SUPPLIER
                request.isCustomer -> CUSTOMER
                else -> ""
            }

            if (type == "") {
                return@transaction
            }

            Supplier.update ({ Supplier.erpCode eq request.odooId.toString() }) {
                it[Supplier.name] = request.name
                it[Supplier.type] = type
            }

            logger.info("Successfully update partner id: $id with new data name: ${request.name}, type: $type")
        }
    }
}