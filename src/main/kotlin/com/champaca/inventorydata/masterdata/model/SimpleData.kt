package com.champaca.inventorydata.masterdata.model

import com.fasterxml.jackson.annotation.JsonInclude

data class SimpleData (
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val id: Int?,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val code: String = "",

    val name: String,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    var data: List<SimpleData>? = null
)