package com.champaca.inventorydata.log.response

import com.champaca.inventorydata.log.usecase.UploadLogDeliveryErrorType

sealed class UploadLogDeliveryResponse{
    data class Success(val count: Int): UploadLogDeliveryResponse()
    data class Failure(
        val type: UploadLogDeliveryErrorType,
        val wrongFormatRefCodes: List<InvalidRefCode>? = null,
        val existingRefCodes: List<InvalidRefCode>? = null,
        val duplicatedRefCodes: List<DuplicatedRefCode>? = null
    ): UploadLogDeliveryResponse()
}