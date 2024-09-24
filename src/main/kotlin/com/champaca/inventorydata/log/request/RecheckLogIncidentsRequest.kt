package com.champaca.inventorydata.log.request

import com.champaca.inventorydata.log.ReceivedLogIncidentSearchParams

data class RecheckLogIncidentsRequest(
    val incidentIds: List<Int>
) {
    fun toReceiveLogIncidentSearchParams(): ReceivedLogIncidentSearchParams {
        return ReceivedLogIncidentSearchParams(ids = incidentIds)
    }
}
