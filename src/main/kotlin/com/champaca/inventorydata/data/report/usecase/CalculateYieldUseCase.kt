package com.champaca.inventorydata.data.report.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.data.report.model.YieldCalculator
import com.champaca.inventorydata.data.report.request.CalculateYieldRequest
import com.champaca.inventorydata.data.report.response.CalculateYieldResponse
import com.champaca.inventorydata.data.report.model.YieldResult
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.RoundingMode
import javax.sql.DataSource

@Service
class CalculateYieldUseCase(
    val dataSource: DataSource,
    val yieldCalculatorMap: Map<Int, YieldCalculator>
) {

    val logger = LoggerFactory.getLogger(CalculateYieldUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(request: CalculateYieldRequest): CalculateYieldResponse {
        Database.connect(dataSource)

        var yieldResults = listOf<YieldResult>()
        transaction {
            addLogger(exposedLogger)

            val calculator = yieldCalculatorMap[request.departmentId]
            if (calculator != null) {
                yieldResults = calculator.calculateYield(request.departmentId, request.fromDate, request.toDate)
            }
        }

        val totalLogs = yieldResults.sumOf { it.incomings }
        val totalLogVolumnFt3 = yieldResults.sumOf { it.incomingVolumnFt3 }
        val totalWoods = yieldResults.sumOf { it.outgoings }
        val totalWoodsVolumnFt3 = yieldResults.sumOf { it.outgoingVolumnFt3 }
        val totalYield = totalWoodsVolumnFt3.multiply(100.toBigDecimal()).divide(totalLogVolumnFt3, 2, RoundingMode.HALF_UP)
        return CalculateYieldResponse(
            yieldResults,
            totalLogs,
            totalLogVolumnFt3,
            totalWoods,
            totalWoodsVolumnFt3,
            totalYield
        )
    }
}