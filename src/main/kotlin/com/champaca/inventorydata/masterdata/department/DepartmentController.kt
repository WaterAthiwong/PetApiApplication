package com.champaca.inventorydata.masterdata.department

import com.champaca.inventorydata.masterdata.department.usecase.GetDepartmentUseCase
import com.champaca.inventorydata.masterdata.department.usecase.GetProcessAndManufacturingLineUseCase
import com.champaca.inventorydata.masterdata.department.usecase.GetProductLineUseCase
import com.champaca.inventorydata.masterdata.model.SimpleData
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/department")
@RestController
@CrossOrigin(origins = ["*"])
class DepartmentController(
    val getDepartmentUseCase: GetDepartmentUseCase,
    val getProcessAndManufacturingLineUseCase: GetProcessAndManufacturingLineUseCase,
    val getProductLineUseCase: GetProductLineUseCase
) {
    @GetMapping("/")
    fun getAllDepartments(): List<SimpleData> {
        return getDepartmentUseCase.execute()
    }

    @GetMapping("/processManuLines/{departmentId}")
    fun getProcessAndManufacturingLine(@PathVariable departmentId: Int): List<SimpleData> {
        return getProcessAndManufacturingLineUseCase.execute(departmentId)
    }

    @GetMapping("/productLine/{departmentId}")
    fun getProductLine(@PathVariable departmentId: Int): List<SimpleData> {
        return getProductLineUseCase.execute(departmentId)
    }
}