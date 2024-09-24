package com.champaca.inventorydata.data.logyard.response

import com.champaca.inventorydata.log.model.StoredLog
import java.math.BigDecimal

data class GetLogsResponse(
    val stocks: List<StoredLog>,
    val totalLogs: Int,
    val totalVolumnM3: BigDecimal,
)