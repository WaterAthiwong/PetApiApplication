package com.champaca.inventorydata.log.request

import com.champaca.inventorydata.log.ReceivedLogIncidentSearchParams

data class GetReceivedLogIncidentsRequest(
    val receivedFrom: String?,
    val receivedTo: String?
) {
    fun toReceivedLogIncidentSearchParams(): ReceivedLogIncidentSearchParams {
        return ReceivedLogIncidentSearchParams(
            createdAtFrom = receivedFrom,
            createdAtTo = receivedTo,
            isSolved = false
        )
    }
}
