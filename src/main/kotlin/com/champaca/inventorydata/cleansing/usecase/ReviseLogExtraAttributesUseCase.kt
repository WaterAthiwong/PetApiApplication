package com.champaca.inventorydata.cleansing.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.databasetable.Log
import com.champaca.inventorydata.databasetable.LogDelivery
import com.champaca.inventorydata.databasetable.LotNo
import com.champaca.inventorydata.databasetable.Supplier
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Deprecated("Use for cleansing log data only")
@Service
class ReviseLogExtraAttributesUseCase(
    val dataSource: DataSource,
) {
    val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun execute() {
        Database.connect(dataSource)
        transaction {
            addLogger(ExposedInfoLogger)
            val logs = findLogs()
            logs.forEach { (lotNoId, log) ->
                LotNo.update({LotNo.id eq lotNoId}) {
                    it[extraAttributes] = log
                }
            }
        }
    }

    private fun findLogs(): Map<Int, Map<String, String>> {
        val joins = Log.join(LotNo, JoinType.INNER) { Log.refCode eq LotNo.refCode }
            .join(LogDelivery, JoinType.INNER) { LogDelivery.id eq Log.logDeliveryId }
            .join(Supplier, JoinType.INNER) { LogDelivery.supplierId eq Supplier.id }
        val query = joins.select(Log.logNo, Log.volumnM3, Log.receivedAt, LotNo.id, LogDelivery.forestryBook,
                LogDelivery.forestryBookNo, Supplier.name)
            .where { (LotNo.status eq "A") and (Log.status eq "A") and (Log.receivedAt.isNotNull()) }
        val result = query.toList().associateBy( { it[LotNo.id].value }, { it }).mapValues { (_, data) ->
            mapOf(
                "logNo" to data[Log.logNo].toString(),
                "volumnM3" to data[Log.volumnM3].setScale(2, RoundingMode.HALF_UP).toString(),
                "forestryBook" to data[LogDelivery.forestryBook],
                "forestryBookNo" to data[LogDelivery.forestryBookNo],
                "supplier" to data[Supplier.name],
                "receivedDate" to if (data[Log.receivedAt] != null) dateFormat.format(data[Log.receivedAt]) else ""
            )
        }
        return result
    }


}