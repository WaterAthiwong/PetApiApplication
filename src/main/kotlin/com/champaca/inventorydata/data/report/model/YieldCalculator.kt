package com.champaca.inventorydata.data.report.model

import java.time.format.DateTimeFormatter

interface YieldCalculator{
    val dateFormat: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun calculateYield(departmentId: Int, startDate: String, endDate: String): List<YieldResult>
}