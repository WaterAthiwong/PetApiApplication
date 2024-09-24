package com.champaca.inventorydata.data.report.model.impl

import com.champaca.inventorydata.data.report.model.YieldCalculator
import com.champaca.inventorydata.log.excel.LogFileDataPosition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class YieldCalculatorConfiguration {

    @Bean
    fun getYieldCalculators(
        sawmillYieldCalculator: SawmillYieldCalculator,
        dailyYieldCalculator: DailyYieldCalculator
    ): Map<Int, YieldCalculator> {
        // The park name e.g. สวนป่าทุ่งเกวียน, สวนป่าน้ำสวยห้วยปลาดุก match with supplier table's "name" column.
        return mapOf(
            1 to sawmillYieldCalculator, // โรงเลื่อย
            4 to dailyYieldCalculator, // ผ่าแปลง
            7 to dailyYieldCalculator, // เตรียมวัตถุดิบไม้พื้น
        )
    }
}