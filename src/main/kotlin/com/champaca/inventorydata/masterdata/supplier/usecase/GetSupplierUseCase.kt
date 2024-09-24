package com.champaca.inventorydata.masterdata.supplier.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.databasetable.dao.SupplierDao.Companion.CUSTOMER
import com.champaca.inventorydata.databasetable.dao.SupplierDao.Companion.SUPCUST
import com.champaca.inventorydata.databasetable.dao.SupplierDao.Companion.SUPPLIER
import com.champaca.inventorydata.masterdata.supplier.SupplierRepository
import com.champaca.inventorydata.masterdata.supplier.model.SupplierData
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetSupplierUseCase(
    val dataSource: DataSource,
    val supplierRepository: SupplierRepository
) {
    val logger = LoggerFactory.getLogger(GetSupplierUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(type: String?): List<SupplierData> {
        Database.connect(dataSource)

        var results = listOf<SupplierData>()
        transaction {
            addLogger(exposedLogger)

            val suppliers = if (type.isNullOrEmpty()) {
                supplierRepository.getAll()
            } else {
                val types = when(type) {
                    SUPPLIER -> listOf(SUPPLIER, SUPCUST)
                    CUSTOMER -> listOf(CUSTOMER, SUPCUST)
                    else -> listOf(type)
                }
                supplierRepository.findByType(listOf(type))
            }

            results = suppliers.map {
                SupplierData(
                    id = it.id.value,
                    name = it.name,
                    taxNo = it.taxNo,
                    address = it.address,
                    phone = it.phone,
                    contact = it.contact,
                    email = it.email,
                    type = it.type,
                    remark = it.remark,
                    erpCode = it.erpCode
                )
            }
        }

        return results
    }
}