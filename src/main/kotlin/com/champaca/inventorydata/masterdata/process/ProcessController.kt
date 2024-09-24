package com.champaca.inventorydata.masterdata.process

import com.champaca.inventorydata.masterdata.process.usecase.GetProcessUseCase
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/process")
@RestController
@CrossOrigin(origins = ["*"])
class ProcessController(
    val getProcessUseCase: GetProcessUseCase
) {
    @GetMapping("/")
    fun getAllProcesses() = getProcessUseCase.exeucte()
}