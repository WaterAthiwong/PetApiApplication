package com.champaca.inventorydata.masterdata.department.usecase

import com.champaca.inventorydata.masterdata.model.SimpleData
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class GetProductLineUseCase(
    val dataSource: DataSource,
    val getProcessAndManufacturingLineUseCase: GetProcessAndManufacturingLineUseCase
) {

    companion object {
        const val R1 = 19
        const val R2 = 20
        const val R3 = 21
        const val R4 = 22
        const val R5 = 23
        const val R6 = 24
        const val R7 = 25

        const val PRESS = 10
        const val FIXING = 13
        const val SANDING = 11
        const val FRENCH_POLISHING = 14
        const val BRUSHING = 15
        const val SIDE_FORMING = 16
        const val HEADER_FOOTER_FORMING = 17
        const val ALL_SIDES_FORMING = 18
        const val FINISHING = 12

        const val RM_DEPARTMENT_ID = 7
        const val FLOORING_DEPARTMENT_ID = 9
    }

    final val rmProductLines = mapOf<String, List<Int>>(
        "Balancer" to listOf(R1, R2, R3),
        "Core" to listOf(R1, R5),
        "Lamella" to listOf(R1, R2, R3, R4),
        "PUR" to listOf(R6, R7),
        "Solid" to listOf(R1, R2)
    )

    final val flooringProductLines = mapOf(
        "ไลน์ไม้พื้น" to listOf(PRESS, FIXING, SANDING, FRENCH_POLISHING, BRUSHING, SIDE_FORMING, HEADER_FOOTER_FORMING, ALL_SIDES_FORMING, FINISHING)
    )

    val productLinesByDept = mapOf<Int, Map<String, List<Int>>>(
        RM_DEPARTMENT_ID to rmProductLines,
        FLOORING_DEPARTMENT_ID to flooringProductLines
    )

    fun execute(departmentId: Int): List<SimpleData> {
        val productLines = productLinesByDept[departmentId]
        if (productLines == null) {
            return emptyList()
        }

        val processManuLineMap = getProcessAndManufacturingLineUseCase.execute(departmentId).associateBy { it.id }
        return productLines.map { (productLineName, processIds) ->
            val processManuLines = processIds.mapNotNull { processManuLineMap[it] }
            SimpleData(
                id = null,
                name = productLineName,
                data = processManuLines
            )
        }
    }
}