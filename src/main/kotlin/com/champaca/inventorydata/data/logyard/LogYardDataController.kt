package com.champaca.inventorydata.data.logyard

import com.champaca.inventorydata.data.logyard.request.GetLogsRequest
import com.champaca.inventorydata.data.logyard.response.GetLogsResponse
import com.champaca.inventorydata.data.logyard.usecase.GetLogsUseCase
import org.springframework.web.bind.annotation.*
import javax.sql.DataSource

@RequestMapping("/logs")
@RestController
@CrossOrigin(origins = ["*"])
class LogYardDataController(
    val dataSource: DataSource,
    val getLogsUseCase: GetLogsUseCase
) {

    @PostMapping(
        value = ["/"], consumes = ["application/json"], produces = ["application/json"])
    fun getLogs(@RequestBody request: GetLogsRequest): GetLogsResponse {
        return getLogsUseCase.execute(request)
    }

}