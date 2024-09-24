package com.champaca.inventorydata.utils

import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class DateTimeUtil {
    fun getCurrentDateTimeString(format: String): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern(format)
        return current.format(formatter)
    }

    fun getYearMonthPrefix(): String {
        val date = LocalDate.now()
        return "${date.year % 2000}${date.month.value.toString().padStart(2, '0')}"
    }
}