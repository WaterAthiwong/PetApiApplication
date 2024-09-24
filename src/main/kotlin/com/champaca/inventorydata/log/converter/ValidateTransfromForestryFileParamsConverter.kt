package com.champaca.inventorydata.log.converter

import com.champaca.inventorydata.log.request.ValidateTransfromForestryFileParams
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class ValidateTransfromForestryFileParamsConverter(
    private val objectMapper: ObjectMapper
): Converter<String, ValidateTransfromForestryFileParams> {

    override fun convert(source: String): ValidateTransfromForestryFileParams {
        return objectMapper.readValue(source, ValidateTransfromForestryFileParams::class.java)
    }
}