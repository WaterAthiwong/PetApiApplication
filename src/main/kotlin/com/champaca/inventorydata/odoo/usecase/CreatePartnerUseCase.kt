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
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class CreatePartnerUseCase(
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

            val id = Supplier.insertAndGetId { supplier ->
                supplier[Supplier.name] = request.name
                supplier[Supplier.type] = type
                supplier[Supplier.erpCode] = request.odooId.toString()
                supplier[Supplier.status] = "A"
            }
            logger.info("Successfully created partner with id: $id, name: ${request.name}, type: $type, erpCode: ${request.odooId}")
        }
    }
}