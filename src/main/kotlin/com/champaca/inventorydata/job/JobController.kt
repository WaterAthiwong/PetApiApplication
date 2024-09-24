package com.champaca.inventorydata.job

import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.job.request.CreateJobRequest
import com.champaca.inventorydata.job.usecase.CreateJobUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/job")
@RestController
@CrossOrigin(origins = ["*"])
class JobController(
    val createJobUseCase: CreateJobUseCase
) {

    @PostMapping("/create")
    fun createJob(@RequestBody request: CreateJobRequest): ResponseEntity<Any> {
        val result = createJobUseCase.execute(request)
        return if (result is ResultOf.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body((result as ResultOf.Failure).message)
        }
    }
}