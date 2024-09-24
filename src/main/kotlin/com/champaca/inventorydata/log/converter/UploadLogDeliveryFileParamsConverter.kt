package com.champaca.inventorydata.log.converter

import com.champaca.inventorydata.log.request.UploadLogDeliveryFileParams
import com.champaca.inventorydata.log.request.ValidateForestryFileParams
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class UploadLogDeliveryFileParamsConverter(
    private val objectMapper: ObjectMapper
): Converter<String, UploadLogDeliveryFileParams> {

    override fun convert(source: String): UploadLogDeliveryFileParams {
        return objectMapper.readValue(source, UploadLogDeliveryFileParams::class.java)
    }
}