package com.champaca.inventorydata.pile.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.PileService
import com.champaca.inventorydata.pile.model.PileEntry
import com.champaca.inventorydata.pile.request.GetPileByDateLine
import com.champaca.inventorydata.pile.response.GetPileByDateLineResponse
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
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.EnumMap
import javax.imageio.ImageIO

@Service
class GetPilesUseCase (
    val dataSource: DataSource,
    val pileService: PileService
) {
    @Value("\${champaca.jasper.location}")
    lateinit var jasperPath: String

    val logger = LoggerFactory.getLogger(GetPilesUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    fun execute(userId: String, request: GetPileByDateLine): GetPileByDateLineResponse {
        Database.connect(dataSource)

        var errorType = PileError.NONE
        var productionDateFrom = request.productionDateFrom
        var productionDateTo = request.productionDateTo
        var manufacturingLineId = request.manufacturingLineId
        var supplierId = request.supplierId
        var departmentId = request.departmentId
        var createdDateFrom = request.createdDateFrom
        var createdDateTo = request.createdDateTo
        var type = request.type
        var pileCodes = request.pileCodes
        
        var pilesGroup: Map<String, List<PileEntry>>? = null
        var pilesResults: MutableList<PileEntry>? = mutableListOf()

        transaction {
            addLogger(exposedLogger)
            val piles = pileService.findPileByDateLineId(userId.toInt(), productionDateFrom, productionDateTo, manufacturingLineId ,supplierId ,pileCodes ,departmentId ,createdDateFrom ,createdDateTo ,type)
            val pileCodes = piles.map { it.code }
            createPileQrCode(pileCodes)
            pilesGroup = piles.groupBy { it.code }
            val initialItemsGroup = pileService.findPileInitialItemsInCodes(pileCodes).groupBy { it.pilecode }
            val currentDatas = pileService.findPileByListCode(userId.toInt(), pileCodes.toList()).groupBy { it.code }

            initialItemsGroup.forEach { (code, initialItems) ->
                pilesGroup!![code]?.let { pilesWithSameCode ->
                    pilesWithSameCode.forEach { pile ->
                        pile.details = initialItems
                    }
                }
            }
            currentDatas.forEach { (code, currentItems) ->
                pilesGroup!![code]?.let { pilesWithSameCode ->
                    pilesWithSameCode.forEach { pile ->
                        pile.currentjobNo = currentItems[0]?.jobNo
                    }
                }
            }

            pilesGroup?.forEach { (code, Piles) ->
                pilesResults?.addAll(Piles)
            }
        }

        return if (pilesGroup == null) {
            GetPileByDateLineResponse.Failure(errorType = errorType,"No Found Pile In Date and ManuLine")
        } else {
            GetPileByDateLineResponse.Success(pilesResults)
        }
    }

    fun createPileQrCode(pileCodes: List<String>){

        pileCodes.forEach { pilecode ->
            val qrPath = jasperPath + "/qr/" + pilecode + ".png"
            val file = File(qrPath)

            if (!file.exists()) {
                generateQrCode(pilecode, qrPath)
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