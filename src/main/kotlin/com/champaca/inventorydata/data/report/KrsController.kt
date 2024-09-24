package com.champaca.inventorydata.data.report

import com.champaca.inventorydata.data.report.request.GetPileTransitionRequest
import com.champaca.inventorydata.data.report.usecase.GetPileTransitionUseCase
import com.champaca.inventorydata.data.report.usecase.krs.*
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/report/krs")
class KrsController(
    val pickForAcqUseCase: PickForAcqUseCase,
    val pickForDryKilnUseCase: PickForDryKilnUseCase,
    val pickForSawUseCase: PickForSawUseCase,
    val receiveFromSawUseCase: ReceiveFromSawUseCase,
    val pickLogUseCase: PickLogUseCase,
    val getPileTransitionUseCase: GetPileTransitionUseCase
) {

    @GetMapping("/pick/log/{pickDateFrom}/{pickDateTo}")
    // เบิกซุง
    fun pickLog(@PathVariable pickDateFrom: String, @PathVariable pickDateTo: String): ResponseEntity<ByteArrayResource> {
        val xlsFile = pickLogUseCase.execute(pickDateFrom, pickDateTo)
        val path: Path = Paths.get(xlsFile.absolutePath)
        val resource = ByteArrayResource(Files.readAllBytes(path))

        return ResponseEntity.ok()
            .contentLength(xlsFile.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource)
    }

    @GetMapping("/pick/saw/{pickDateFrom}/{pickDateTo}")
    // เบิกเข้าผ่าโรงเลื่อย
    fun pickForSaw(@PathVariable pickDateFrom: String, @PathVariable pickDateTo: String): ResponseEntity<ByteArrayResource> {
        val xlsFile = pickForSawUseCase.execute(pickDateFrom, pickDateTo)
        val path: Path = Paths.get(xlsFile.absolutePath)
        val resource = ByteArrayResource(Files.readAllBytes(path))

        return ResponseEntity.ok()
            .contentLength(xlsFile.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource)
    }

    @GetMapping("/receive/saw/{receiveDateFrom}/{receiveDateTo}")
    // รับจากผลิตโรงเลื่อย
    fun receiveFromSaw(@PathVariable receiveDateFrom: String, @PathVariable receiveDateTo: String): ResponseEntity<ByteArrayResource> {
        val xlsFile = receiveFromSawUseCase.execute(receiveDateFrom, receiveDateTo)
        val path: Path = Paths.get(xlsFile.absolutePath)
        val resource = ByteArrayResource(Files.readAllBytes(path))

        return ResponseEntity.ok()
            .contentLength(xlsFile.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource)
    }

    @GetMapping("/pick/acq/{pickDateFrom}/{pickDateTo}")
    // เบิกเข้าอัดน้ำยา
    fun pickForAcq(@PathVariable pickDateFrom: String, @PathVariable pickDateTo: String): ResponseEntity<ByteArrayResource> {
        val xlsFile = pickForAcqUseCase.execute(pickDateFrom, pickDateTo)
        val path: Path = Paths.get(xlsFile.absolutePath)
        val resource = ByteArrayResource(Files.readAllBytes(path))

        return ResponseEntity.ok()
            .contentLength(xlsFile.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource)
    }

    @GetMapping("/pick/drykiln/{pickDateFrom}/{pickDateTo}")
    // เบิกเข้าเตาอบ
    fun pickForDryKiln(@PathVariable pickDateFrom: String, @PathVariable pickDateTo: String): ResponseEntity<ByteArrayResource> {
        val xlsFile = pickForDryKilnUseCase.execute(pickDateFrom, pickDateTo)
        val path: Path = Paths.get(xlsFile.absolutePath)
        val resource = ByteArrayResource(Files.readAllBytes(path))

        return ResponseEntity.ok()
            .contentLength(xlsFile.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource)
    }

    @PostMapping("/pile/transition")
    fun getTransition(@RequestBody request: GetPileTransitionRequest): ResponseEntity<ByteArrayResource> {
        val xlsFile = getPileTransitionUseCase.executeToKrsFile(request)
        val path: Path = Paths.get(xlsFile.absolutePath)
        val resource = ByteArrayResource(Files.readAllBytes(path))

        return ResponseEntity.ok()
            .contentLength(xlsFile.length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource)
    }
}