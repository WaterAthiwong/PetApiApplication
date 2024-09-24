package com.champaca.inventorydata.masterdata.skugroup

import com.champaca.inventorydata.masterdata.skugroup.response.SkuGroupData
import com.champaca.inventorydata.masterdata.skugroup.response.SkuGroupMainData
import com.champaca.inventorydata.masterdata.skugroup.usecase.GetSkuGroupByMainUseCase
import com.champaca.inventorydata.masterdata.skugroup.usecase.GetSkuGroupUseCase
import com.champaca.inventorydata.masterdata.skugroup.usecase.GetSkuMainGroupUseCase
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/skuGroup")
@RestController
@CrossOrigin(origins = ["*"])
class SkuGroupController(
    val getSkuGroupUseCase: GetSkuGroupUseCase,
    val getSkuMainGroupUseCase: GetSkuMainGroupUseCase,
    val getSkuGroupByMainUseCase: GetSkuGroupByMainUseCase
) {

    @GetMapping("/")
    fun getAllSkuGroup(): List<SkuGroupData> {
        return getSkuGroupUseCase.execute("")
    }

    @GetMapping("/{processPrefix}")
    fun getSkuGroup(@PathVariable processPrefix: String): List<SkuGroupData> {
        return getSkuGroupUseCase.execute(processPrefix)
    }

    @GetMapping("/main/{processPrefix}")
    fun getSkuMainGroup(@PathVariable processPrefix: String): List<String> {
        return getSkuMainGroupUseCase.execute(processPrefix)
    }

    @GetMapping("/main/sub/{mainGroup}")
    fun getSkuGroupByMainId(@PathVariable mainGroup: String): List<SkuGroupMainData> {
        return getSkuGroupByMainUseCase.execute(mainGroup)
    }
}