package com.champaca.inventorydata.pile.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.model.PileEntry
import com.champaca.inventorydata.pile.model.LotDetailInPile
import com.champaca.inventorydata.pile.model.MovingItem
import com.champaca.inventorydata.pile.request.PrintPileRequest
import com.champaca.inventorydata.pile.response.GetPilesLotsResponse
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import javax.sql.DataSource
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import net.sf.jasperreports.engine.*
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource
import net.sf.jasperreports.engine.xml.JRXmlLoader
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Paths
import java.util.EnumMap
import javax.imageio.ImageIO

@Service
class GetPilesPrintUseCase (
    val dataSource: DataSource,
    val pileService: PileService
) {
    @Value("\${champaca.jasper.location}")
    lateinit var jasperPath: String

    val logger = LoggerFactory.getLogger(GetPilesPrintUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(userId: String, request : PrintPileRequest): GetPilesLotsResponse {
        Database.connect(dataSource)

        val piles = request.pileCodes
        val errorType = PileError.PILE_NOT_FOUND
        var pileEntrys: List<PileEntry>? = null
        transaction {
            addLogger(exposedLogger)
            pileEntrys = pileService.findPileByListCode(userId.toInt(), piles)
            val pilecodes = pileEntrys!!.map { it.code }
            createPileQr(pilecodes)
            val pilesGroup = pileEntrys!!.groupBy { it.code }
            var initialItemsGroup: Map<String, List<MovingItem>> = emptyMap()

            if(request.format=="shelf"){
                initialItemsGroup = pileService.findShelfItemsInCodes(pilecodes).groupBy { it.pilecode }
            }else{
                initialItemsGroup = pileService.findPileInitialItemsInCodes(pilecodes).groupBy { it.pilecode }
            }


            initialItemsGroup.forEach { (code, initialItems) ->
                pilesGroup[code]?.let { pilesWithSameCode ->
                    pilesWithSameCode.forEach { pile ->
                        pile.details = initialItems
                    }
                }
            }

            pileService.updatedPilePrintedAt(pilecodes)
            createPileQr(pilecodes)
            pilesGroup.forEach { (code, piles) ->
                piles.forEach{pile->

                    pile.preferredMeasurement = request.preferredMeasurement
                    pile.sku = if(pile.details?.isNotEmpty() == true) pile.details!!.get(0).species.toString() else " "
                    pile.type = if(pile.details?.isNotEmpty() == true) pile.details!!.get(0).erpGroupCode.toString()+" "+ pile.details!!.get(0).skuGroupName.toString() else ""
                    pile.fsc =
                        if(pile.details?.isNotEmpty() == true) {
                            if(pile.details!!.get(0).fsc == true) {
                                "FSC"
                            }else{
                                "NON FSC"
                            }
                        }
                        else{ "null" }

                    pile.grade = if(pile.details?.isNotEmpty() == true) pile.details!!.get(0).grade.toString() else " "
                    pile.matCode = if(pile.details?.isNotEmpty() == true) pile.details!!.get(0).matCode.toString() else " "

                    val lots: List<LotDetailInPile?>? = pile.details?.map { item ->
                        item.grade?.let {
                            LotDetailInPile(
                                size = item.matCode.substringAfter('-').replace("x", "X").replace("X", " X "),
                                width = item.width,
                                widthUom = item.widthUom,
                                length = item.length,
                                lengthUom = item.lengthUom,
                                thickness = item.thickness,
                                thicknessUom = item.thicknessUom,
                                volumeFt3 = item.volumnFt3.multiply(item.qty),
                                volumeM3 = item.volumnM3.multiply(item.qty),
                                areaM2 = item.areaM2.multiply(item.qty),
                                grade = it,
                                qty = item.qty,
                                fsc = if(item.fsc) "FSC" else "NON FSC",
                                group = item.skuGroupName,
                            )
                        }
                    }
                    pile.lots = lots as List<LotDetailInPile>?
                }
            }

        }

        return if (pileEntrys==null) {
            GetPilesLotsResponse.Failure(errorType = errorType,"NOTFOUNDPILE")
        } else {
            GetPilesLotsResponse.Success(pileEntrys)
        }
    }

    fun ReportjasperSetting(result: GetPilesLotsResponse.Success, request: PrintPileRequest): ResponseEntity<ByteArray> {
        val dataSource = reportParamFormat(result, request)
        val type = request.format
        var path = ""

        if(type != "shelf"){
            path = jasperPath + "/pileSheet.jrxml"
        }
        else{
            path = jasperPath + "/shelfSheet.jrxml"
        }

        val filePath = Paths.get(path)
        val input = Files.newInputStream(filePath)
        val jasperDesign = JRXmlLoader.load(input)

        val jasperReport = JasperCompileManager.compileReport(jasperDesign)

        val jrContext: JasperReportsContext = DefaultJasperReportsContext.getInstance()
        val fontpath = jasperPath + "/fonts/THSarabunNew Bold.ttf"

        JRPropertiesUtil.getInstance(jrContext).setProperty("net.sf.jasperreports.default.font.name","TH Sarabun New");
        JRPropertiesUtil.getInstance(jrContext).setProperty("net.sf.jasperreports.default.pdf.font.name",fontpath);
        JRPropertiesUtil.getInstance(jrContext).setProperty("net.sf.jasperreports.default.pdf.encoding", "Identity-H");

        val parameters = HashMap<String, Any>()
        val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, JRMapCollectionDataSource(
            dataSource as Collection<MutableMap<String, *>>?
        )
        )

        val byteArrayOutputStream = ByteArrayOutputStream()
        JasperExportManager.exportReportToPdfStream(jasperPrint, byteArrayOutputStream)

        val pdfBytes: ByteArray = byteArrayOutputStream.toByteArray()

        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf")
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=piles.pdf")

        return ResponseEntity(pdfBytes, headers, HttpStatus.OK)
    }

    fun reportParamFormat(pileList: GetPilesLotsResponse.Success, request: PrintPileRequest): MutableList<Map<String, Any>> {
        val dataSource = mutableListOf<Map<String, Any>>()
        val path = jasperPath+"/qr/"

//
        for (pile in pileList.piles!!) {
            if(request.format!="shelf") {
                var detailData = pile.lots.orEmpty().map {
                    mapOf(
                        "unitType" to pile.preferredMeasurement.toString(),
                        "size" to it.size,
                        "width" to it.width,
                        "widthUom" to it.widthUom,
                        "length" to it.length,
                        "lengthUom" to it.lengthUom,
                        "thickness" to it.thickness,
                        "thicknessUom" to it.thicknessUom,
                        "volumeFt3" to it.volumeFt3,
                        "volumeM3" to it.volumeM3,
                        "areaM2" to it.areaM2,
                        "grade" to it.grade,
                        "qty" to it.qty,
                        "fsc" to it.fsc,
                        "group" to it.group,
                    )
                }
                val manualData = mapOf(
                    "unitType" to pile.preferredMeasurement.toString(),
                    "size" to "",
                    "width" to 0.00.toBigDecimal(),
                    "widthUom" to "",
                    "length" to 0.00.toBigDecimal(),
                    "lengthUom" to "",
                    "thickness" to 0.00.toBigDecimal(),
                    "thicknessUom" to "",
                    "volumeFt3" to (pile.lots?.sumOf { it.volumeFt3 } ?: 0.toBigDecimal()),
                    "volumeM3" to 0.00.toBigDecimal(),
                    "areaM2" to (pile.lots?.sumOf { it.areaM2 ?: BigDecimal.ZERO } ?: BigDecimal.ZERO),
                    "grade" to "TOTAL",
                    "qty" to (pile.lots?.sumOf { it.qty } ?: 0.toBigDecimal()),
                    "fsc" to "",
                    "group" to "",
                )

                detailData = detailData + listOf(manualData)

                val pileData = mapOf(
                    "pile" to pile.code.toString(),
                    "date" to pile.productionDate.toString(),
                    "departmentid" to pile.currentDepartmentId.toString(),
                    "supplier" to pile.suppliername.toString(),
                    "lineid" to pile.manufacturingLinename.toString(),
                    "po" to pile.poNo.toString(),
                    "job" to pile.jobNo.toString(),
                    "lot" to pile.lotNo.toString(),
                    "remark" to pile.remark.toString(),
                    "sku" to pile.sku.toString(),
                    "type" to pile.type.toString(),
                    "fsc" to pile.fsc.toString(),
                    "invoice" to pile.invoiceNo.toString(),
                    "order" to pile.orderNo.toString(),
                    "mattype" to "",
                    "creator" to pile.creator.toString(),
                    "QrPath" to path,
                    "QrName" to pile.code+".png",
                    "detail" to detailData,
                    "createdAt" to pile.createdAt,
                    "grade" to pile.grade,
                    "matCode" to pile.matCode,
                    "customer" to pile.customer,
                    "countryOfOrigin" to pile.countryOfOrigin,
                )
                dataSource.add(pileData as Map<String, Any>)
            }
            else{
                var detailData = mutableListOf<Map<String, Any>>()

                var detailData2 = pile.lots.orEmpty().map {
                    mapOf(
                        "unitType" to pile.preferredMeasurement.toString(),
                        "size" to it.size,
                        "width" to it.width,
                        "widthUom" to it.widthUom,
                        "length" to it.length,
                        "lengthUom" to it.lengthUom,
                        "thickness" to it.thickness,
                        "thicknessUom" to it.thicknessUom,
                        "volumeFt3" to it.volumeFt3,
                        "volumeM3" to it.volumeM3,
                        "areaM2" to it.areaM2,
                        "grade" to it.grade,
                        "qty" to it.qty,
                        "fsc" to it.fsc,
                        "group" to it.group,
                    )
                }

                detailData = (detailData + detailData2) as MutableList<Map<String, Any>>

                val manualData = mapOf(
                    "unitType" to pile.preferredMeasurement.toString(),
                    "size" to "",
                    "width" to 0.00.toBigDecimal(),
                    "widthUom" to "",
                    "length" to 0.00.toBigDecimal(),
                    "lengthUom" to "",
                    "thickness" to 0.00.toBigDecimal(),
                    "thicknessUom" to "",
                    "volumeFt3" to (pile.lots?.sumOf { it.volumeFt3 } ?: 0.toBigDecimal()),
                    "volumeM3" to 0.00.toBigDecimal(),
                    "areaM2" to (pile.lots?.sumOf { it.areaM2 ?: BigDecimal.ZERO } ?: BigDecimal.ZERO),
                    "grade" to "TOTAL",
                    "qty" to (pile.lots?.sumOf { it.qty } ?: 0.toBigDecimal()),
                    "fsc" to "",
                    "group" to "",
                )

                detailData = (detailData + manualData).toMutableList()

                if(detailData.size<7){
                    repeat(7-detailData.size) {
                        val manualData = mapOf(
                            "unitType" to pile.preferredMeasurement.toString(),
                            "size" to "",
                            "width" to 0.00.toBigDecimal(),
                            "widthUom" to "",
                            "length" to 0.00.toBigDecimal(),
                            "lengthUom" to "",
                            "thickness" to 0.00.toBigDecimal(),
                            "thicknessUom" to "",
                            "volumeFt3" to 0.toBigDecimal(),
                            "volumeM3" to 0.00.toBigDecimal(),
                            "areaM2" to BigDecimal.ZERO,
                            "grade" to "BLANK",
                            "qty" to 0.toBigDecimal(),
                            "fsc" to "",
                            "group" to ""
                        )
                        detailData.add(manualData)
                    }
                }

                val pileData = mapOf(
                    "pile" to pile.code.toString(),
                    "date" to pile.productionDate.toString(),
                    "departmentid" to pile.currentDepartmentId.toString(),
                    "supplier" to pile.suppliername.toString(),
                    "lineid" to pile.manufacturingLinename.toString(),
                    "po" to pile.poNo.toString(),
                    "job" to pile.jobNo.toString(),
                    "lot" to pile.lotNo.toString(),
                    "remark" to pile.remark.toString(),
                    "sku" to pile.sku.toString(),
                    "type" to pile.type.toString(),
                    "fsc" to pile.fsc.toString(),
                    "invoice" to pile.invoiceNo.toString(),
                    "order" to pile.orderNo.toString(),
                    "mattype" to "",
                    "creator" to pile.creator.toString(),
                    "QrPath" to path,
                    "QrName" to pile.code+".png",
                    "detail" to detailData,
                    "createdAt" to pile.createdAt,
                    "grade" to pile.grade,
                    "matCode" to pile.matCode,
                    "customer" to pile.customer,
                    "countryOfOrigin" to pile.countryOfOrigin,
                )
                dataSource.add(pileData as Map<String, Any>)
            }
        }

        return dataSource
    }

    fun createPileQr(pileCodes: List<String>){
        pileCodes.forEach { pilecode ->
            val qrPath = jasperPath + "/qr/" + pilecode + ".png"
            val file = File(qrPath)

            if (!file.exists()) {
                val filePath = qrPath
                generateQrCode(pilecode, filePath)
            }
        }
    }

    fun generateQrCode(text: String, filePath: String) {
        try {
            val hintMap: MutableMap<EncodeHintType, Any> = EnumMap(EncodeHintType::class.java)
            hintMap[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H

            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 300, 300, hintMap)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    image.setRGB(x, y, if (bitMatrix[x, y]) Color.BLACK.rgb else Color.WHITE.rgb)
                }
            }

            val file = File(filePath)
            ImageIO.write(image, "png", file)

        } catch (e: Exception) {
            logger.error("Error while generating QR code", e)
        }
    }
}