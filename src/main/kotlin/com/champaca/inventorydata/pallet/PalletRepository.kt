package com.champaca.inventorydata.pallet

import com.champaca.inventorydata.databasetable.Pallet
import com.champaca.inventorydata.databasetable.dao.PalletDao
import org.jetbrains.exposed.sql.and
import org.springframework.stereotype.Service

@Service
class PalletRepository {

    fun findByCode(code: String): PalletDao? {
        return PalletDao.find { (Pallet.code eq code) and (Pallet.status eq "A") }.singleOrNull()
    }
}