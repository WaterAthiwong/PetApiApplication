package com.champaca.inventorydata.log.converter

import com.champaca.inventorydata.log.request.ValidateForestryFileParams
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class ValidateForestryFileParamsConverter(
    private val objectMapper: ObjectMapper
): Converter<String, ValidateForestryFileParams> {

    override fun convert(source: String): ValidateForestryFileParams {
        return objectMapper.readValue(source, ValidateForestryFileParams::class.java)
    }
}