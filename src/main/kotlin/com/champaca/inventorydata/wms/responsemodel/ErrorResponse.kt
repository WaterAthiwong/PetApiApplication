package com.champaca.inventorydata.wms.responsemodel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val header: Header,

    @SerialName("error_messages")
    val errorMessages: Map<String, List<String>>
) {
    fun consolidatedMessage(delimiter: String = "\n"): String {
        val builder = StringBuilder()
        errorMessages.forEach { key, values ->
            values.forEach { value ->
                builder.append("$key:$value$delimiter")
            }
        }
        return builder.toString()
    }
}
