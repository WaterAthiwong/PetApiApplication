package com.champaca.inventorydata.log.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.log.LogDeliveryError
import com.champaca.inventorydata.log.LogDeliveryService
import com.champaca.inventorydata.log.LogSearchParams
import com.champaca.inventorydata.masterdata.user.UserRepository
import com.champaca.inventorydata.log.request.RecheckLogIncidentsRequest
import com.champaca.inventorydata.log.response.RecheckLogIncidentResponse
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class RecheckBarcodeUseCase(
    val dataSource: DataSource,
    val logDeliveryService: LogDeliveryService,
    val userRepository: UserRepository
) {
    fun execute(username: String, request: RecheckLogIncidentsRequest): RecheckLogIncidentResponse {
        val successBarcodes: MutableList<String> = mutableListOf()
        val failedBarcodes: MutableList<String> = mutableListOf()
        var errorType: LogDeliveryError? = null
        var userId = -1

        Database.connect(dataSource)
        transaction {
            addLogger(ExposedInfoLogger)

            val user = userRepository.findByUsername(username)
            if (user == null) {
                errorType = LogDeliveryError.NO_USERNAME_IN_WMS
                return@transaction
            }

            userId = user!!.id.value

            val logIncidents = logDeliveryService.getReceivedLogIncidents(request.toReceiveLogIncidentSearchParams())
            val barcodes = logIncidents.filter { it.errorType != LogDeliveryError.NON_EXISTING_BARCODE }.map { it.barcode }.toSet().toList() as List<String>
            val logs = logDeliveryService.getLogs(LogSearchParams(refCodes = barcodes))
            val barcodeMap = logs.associateBy({it.refCode}, {it})


            logIncidents.forEach { logIncident ->
                val log = barcodeMap[logIncident.barcode]
                if (log != null) {
                    logIncident.isSolved = true
                    logDeliveryService.updateReceiveLogIncident(logIncident)

                    log.apply {
                        this.errorType = null
                        this.receivedAt = logIncident.createdAt
                        this.receivingUserId = logIncident.userId
                    }
                    logDeliveryService.updateLog(log)
                    successBarcodes.add(log.refCode)
                } else {
                    failedBarcodes.add(logIncident.barcode)
                }
            }
        }

        return if (errorType != null) {
            RecheckLogIncidentResponse.Failure(errorType = errorType!!)
        } else {
            RecheckLogIncidentResponse.Success(
                successBarcodes = successBarcodes,
                failedBarcodes = failedBarcodes
            )
        }
    }
}