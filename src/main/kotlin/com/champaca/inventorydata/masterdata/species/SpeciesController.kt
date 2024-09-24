package com.champaca.inventorydata.masterdata.species

import com.champaca.inventorydata.masterdata.model.SimpleData
import com.champaca.inventorydata.masterdata.species.usecase.GetSpeciesUseCase
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/species")
@RestController
@CrossOrigin(origins = ["*"])
class SpeciesController(
    val getSpeciesUseCase: GetSpeciesUseCase
) {
    @GetMapping("/{processPrefix}")
    fun getSpeciesForProcess(@PathVariable processPrefix: String): List<SimpleData> {
        return getSpeciesUseCase.execute(processPrefix)
    }

    @GetMapping("/")
    fun getAllSpecies(): List<SimpleData> {
        return getSpeciesUseCase.execute("")
    }
}