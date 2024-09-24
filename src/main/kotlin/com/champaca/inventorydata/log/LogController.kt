package com.champaca.inventorydata.log

import com.champaca.inventorydata.common.ChampacaConstant.USERNAME
import com.champaca.inventorydata.common.ChampacaConstant.USER_ID
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.log.model.UploadedLog
import com.champaca.inventorydata.log.request.*
import com.champaca.inventorydata.log.response.*
import com.champaca.inventorydata.log.usecase.*
import com.champaca.inventorydata.masterdata.supplier.SupplierRepository
import com.champaca.inventorydata.wms.WmsService.Companion.WMS_SESSION
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.sql.DataSource

@RequestMapping("/logs")
@RestController
@CrossOrigin(origins = ["*"])
class LogController(
    val dataSource: DataSource,
    val logService: LogService,
    val supplierRepository: SupplierRepository,
    val createPickedLogFileUseCase: CreatePickedLogFileUseCase,
    val validateForestryFileUseCase: ValidateForestryFileUseCase,
    val uploadLogDeliveryUseCase: UploadLogDeliveryUseCase,
    val receiveLogUseCase: ReceiveLogUseCase,
    val getDeliveryLogFileColumnsUseCase: GetDeliveryLogFileColumnsUseCase,
    val getLogDetailUseCase: GetLogDetailUseCase,
    val getUploadedLogsUseCase: GetUploadedLogsUseCase,
    val getReceivedLogIncidentsUseCase: GetReceivedLogIncidentsUseCase,
    val recheckMatCodeUseCase: RecheckMatCodeUseCase,
    val recheckBarcodeUseCase: RecheckBarcodeUseCase,
    val getForestryBooksUseCase: GetForestryBooksUseCase,
    val createUploadedLogsReportUseCase: CreateUploadedLogsReportUseCase,
    val createLogsInWmsUseCase: CreateLogsInWmsUseCase
) {
    @PostMapping(
        value = ["/refCode/existing"], consumes = ["application/json"], produces = ["application/json"])
    fun checkExistingRefCodes(@RequestBody request: CheckExistingRefCodesRequest): CheckExistingRefCodesResponse {
        var results: List<String> = listOf()
        Database.connect(dataSource)
        transaction {
            results = logService.getExistingRefCodes(request.refCodes)
        }
        return CheckExistingRefCodesResponse(results)
    }

    @PostMapping(
        value = ["/received/pickedFile"], consumes = ["application/json"])
    fun createPickedLogFile(@RequestBody request: CreatePickedLogFileRequest): ResponseEntity<ByteArrayResource> {
        val xlsFile = createPickedLogFileUseCase.execute(request)
        val path: Path = Paths.get(xlsFile.absolutePath)
        val resource = ByteArrayResource(Files.readAllBytes(path))

        return ResponseEntity.ok()
            .contentLength(xlsFile.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource)
    }

    @PostMapping("/forestryFile/validate")
    fun validateForestryFile(@RequestParam("file") file: MultipartFile, @RequestParam("params") params: ValidateForestryFileParams): ValidateForestryFileResponse {
        return validateForestryFileUseCase.validate(file, params)
    }

    @PostMapping("/forestryFile/validateTransform")
    fun validateAndTransformForestryFile(@RequestParam("file") file: MultipartFile, @RequestParam("params") params: ValidateTransfromForestryFileParams): ResponseEntity<ByteArrayResource> {
        val result = validateForestryFileUseCase.validateAndTransform(file, params)
        return if(result is ResultOf.Success) {
            val path: Path = Paths.get(result.value.absolutePath)
            val resource = ByteArrayResource(Files.readAllBytes(path))
            ResponseEntity.ok()
                .contentLength(result.value.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource)
        } else {
            val errorResult = result as ResultOf.Failure
            ResponseEntity.badRequest()
                .contentLength(errorResult.message!!.length.toLong())
                .contentType(MediaType.TEXT_PLAIN)
                .body(ByteArrayResource(errorResult.message!!.toByteArray(StandardCharsets.UTF_8)))
        }
    }

    @PostMapping("/delivery/upload")
    fun uploadLogDelivery(@RequestParam("file") file: MultipartFile, @RequestParam("params") params: UploadLogDeliveryFileParams): ResponseEntity<Any> {
        val result = uploadLogDeliveryUseCase.execute(file, params)
        return if (result is UploadLogDeliveryResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/delivery/receiveLog")
    fun receiveLog(@RequestHeader(USERNAME) username: String, @RequestBody request: ReceiveLogRequest): ResponseEntity<Any> {
        val result = receiveLogUseCase.execute(username, request)
        return if (result is ReceivedLogResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @GetMapping("/delivery/columns/{supplierId}")
    fun getDeliveryLogFileColumns(@PathVariable supplierId: Int): ResponseEntity<Any> {
        val result = getDeliveryLogFileColumnsUseCase.execute(supplierId)
        return if (result != null) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body("No Such Supplier")
        }
    }

    @GetMapping("/detail/{barcode}")
    fun getLogDetail(@PathVariable barcode: String): ResponseEntity<Any> {
        val result = getLogDetailUseCase.execute(barcode)
        return if (result is GetLogDetailResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/delivery/uploaded")
    fun getUploadedLogs(@RequestBody request: GetUploadedLogsRequest): List<UploadedLog> {
        return getUploadedLogsUseCase.execute(request)
    }

    @PostMapping("/delivery/uploaded/report")
    fun createUploadedLogReport(@RequestBody request: CreateUploadedLogsReportRequest): ResponseEntity<Any> {
        val file = createUploadedLogsReportUseCase.exeucte(request)
        val path = Paths.get(file.absolutePath)
        val resource = ByteArrayResource(Files.readAllBytes(path))
        return ResponseEntity.ok()
            .contentLength(file.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource)
    }

    @PostMapping("/delivery/receiveLog/incident/")
    fun getReceiveLogIncidents(@RequestBody request: GetReceivedLogIncidentsRequest): List<UploadedLog> {
        return getReceivedLogIncidentsUseCase.execute(request)
    }

    @PostMapping("/delivery/receiveLog/incident/recheck/matCode")
    fun recheckMatCode(@RequestHeader(USERNAME) username: String, @RequestBody request: RecheckLogIncidentsRequest): ResponseEntity<Any> {
        val result = recheckMatCodeUseCase.execute(username, request)
        return if (result is RecheckLogIncidentResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/delivery/receiveLog/incident/recheck/barcode")
    fun recheckBarcode(@RequestHeader(USERNAME) username: String, @RequestBody request: RecheckLogIncidentsRequest): ResponseEntity<Any> {
        val result = recheckBarcodeUseCase.execute(username, request)
        return if (result is RecheckLogIncidentResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @GetMapping("/delivery/forestryBook")
    fun getForestryBooks(): GetForestryBooksResponse {
        return getForestryBooksUseCase.execute()
    }

    @PostMapping("/delivery/wms/createLog")
    fun createLogsInWms(@RequestAttribute(WMS_SESSION) session: String,
                        @RequestAttribute(USER_ID) userId: String,
                        @RequestBody request: CreateLogsInWmsRequest): ResponseEntity<Any> {
        val result = createLogsInWmsUseCase.execute(session, userId, request)
        return if (result is CreateLogsInWmsResponse.Success) {
            ResponseEntity.ok().body(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }
}