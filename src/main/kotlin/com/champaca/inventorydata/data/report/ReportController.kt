package com.champaca.inventorydata.data.report

import com.champaca.inventorydata.data.report.request.CalculateYieldRequest
import com.champaca.inventorydata.data.report.request.GetManufacturingLineOutputRequest
import com.champaca.inventorydata.data.report.request.GetPileTransactionRequest
import com.champaca.inventorydata.data.report.request.GetSawedLogRequest
import com.champaca.inventorydata.data.report.response.CalculateYieldResponse
import com.champaca.inventorydata.data.report.response.GetKilnStatusResponse
import com.champaca.inventorydata.data.report.response.PileTransactionEntry
import com.champaca.inventorydata.data.report.response.ProductionOutputResponse
import com.champaca.inventorydata.data.report.usecase.*
import com.champaca.inventorydata.model.ProcessedLog
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@RestController
@RequestMapping("/report")
@CrossOrigin(origins = ["*"])
class ReportController(
    val getPileTransactionUseCase: GetPileTransactionUseCase,
    val calculateYieldUseCase: CalculateYieldUseCase,
    val getSawedLogUsecase: GetSawedLogUsecase,
    val getKilnStatusUseCase: GetKilnStatusUseCase,
    val getManufacturingLineOutputUseCase: GetManufacturingLineOutputUseCase
) {
    @PostMapping("/pile/pick/excel/perJob")
    fun getPickTransactionExcelPerJobReport(@RequestBody request: GetPileTransactionRequest): ResponseEntity<ByteArrayResource> {
        val xlsFile =  getPileTransactionUseCase.executeExcelPerJobReport(request, true)
        val path: Path = Paths.get(xlsFile.absolutePath)
        val resource = ByteArrayResource(Files.readAllBytes(path))
        return ResponseEntity.ok()
            .contentLength(xlsFile.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource)
    }

    @PostMapping("/pile/pick/excel/allJobs")
    fun getPickTransactionExcelAllJobsReport(@RequestBody request: GetPileTransactionRequest): ResponseEntity<ByteArrayResource> {
        val xlsFile =  getPileTransactionUseCase.executeExcelAllJobsReport(request, true)
        val path: Path = Paths.get(xlsFile.absolutePath)
        val resource = ByteArrayResource(Files.readAllBytes(path))
        return ResponseEntity.ok()
            .contentLength(xlsFile.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource)
    }

    @PostMapping("/pile/receive/excel/perJob")
    fun getReceiveTransactionExcelEachJobReport(@RequestBody request: GetPileTransactionRequest): ResponseEntity<ByteArrayResource> {
        val xlsFile =  getPileTransactionUseCase.executeExcelPerJobReport(request, false)
        val path: Path = Paths.get(xlsFile.absolutePath)
        val resource = ByteArrayResource(Files.readAllBytes(path))
        return ResponseEntity.ok()
            .contentLength(xlsFile.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource)
    }

    @PostMapping("/pile/receive/excel/allJobs")
    fun getReceiveTransactionExcelAllJobsReport(@RequestBody request: GetPileTransactionRequest): ResponseEntity<ByteArrayResource> {
        val xlsFile =  getPileTransactionUseCase.executeExcelAllJobsReport(request, false)
        val path: Path = Paths.get(xlsFile.absolutePath)
        val resource = ByteArrayResource(Files.readAllBytes(path))
        return ResponseEntity.ok()
            .contentLength(xlsFile.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource)
    }

    @PostMapping("/yield")
    fun calculateYield(@RequestBody request: CalculateYieldRequest): CalculateYieldResponse {
        return calculateYieldUseCase.execute(request)
    }

    @PostMapping("/log/sawed")
    fun getSawedLogs(@RequestBody request: GetSawedLogRequest): List<ProcessedLog> {
        return getSawedLogUsecase.execute(request)
    }

    @GetMapping("/kiln/status")
    fun getKilnStatus(): GetKilnStatusResponse {
        return getKilnStatusUseCase.execute()
    }

    @PostMapping("/production/output")
    fun getManufacturingLineOutput(@RequestBody request: GetManufacturingLineOutputRequest): ResponseEntity<Any> {
        if (request.isBlank()) {
            return ResponseEntity.badRequest().body("Invalid request")
        }
        return ResponseEntity.ok(getManufacturingLineOutputUseCase.execute(request))
    }
}