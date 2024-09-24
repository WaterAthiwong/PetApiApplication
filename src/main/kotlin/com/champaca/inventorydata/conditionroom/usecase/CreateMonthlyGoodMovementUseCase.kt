package com.champaca.inventorydata.conditionroom.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.ExposedLogger
import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.conditionroom.ConditionRoomController.Companion.CONDITION_ROOM_INCOMING_PICK_PREFIX
import com.champaca.inventorydata.conditionroom.ConditionRoomController.Companion.CONDITION_ROOM_INCOMING_RECEIVE_PREFIX
import com.champaca.inventorydata.conditionroom.ConditionRoomController.Companion.CONDITION_ROOM_OUTGOING_PICK_PREFIX
import com.champaca.inventorydata.conditionroom.ConditionRoomController.Companion.CONDITION_ROOM_OUTGOING_RECEIVE_PREFIX
import com.champaca.inventorydata.databasetable.Config
import com.champaca.inventorydata.databasetable.GoodMovement
import com.champaca.inventorydata.databasetable.dao.GoodMovementDao
import com.champaca.inventorydata.goodmovement.model.GoodMovementData
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.utils.DateTimeUtil
import com.champaca.inventorydata.wms.CryptoService
import com.champaca.inventorydata.wms.WmsService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

@Service
class CreateMonthlyGoodMovementUseCase(
    val dataSource: DataSource,
    val wmsService: WmsService,
    val cryptoService: CryptoService,
    val dateTimeUtil: DateTimeUtil
) {
    companion object {
        val LAST_ACTIVE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")
        val CONDITION_ROOM_DEPARTMENT_ID = 6
        val CHAMPACA_SUPPLIER_ID = 18
    }

    val logger = LoggerFactory.getLogger(CreateMonthlyGoodMovementUseCase::class.java)
    val exposedLogger = ExposedLogger(logger)

    @Value("\${wms.admin.username}")
    lateinit var adminUsername: String

    @Value("\${wms.admin.userId}")
    lateinit var adminUserId: String

    // Use Case นี้ใช้สำหรับ Cron Job ที่จะตั้งไว้ให้ Run วันที่ 1 ของทุกเดือน เพื่อสร้างใบรับ และใบเบิกอย่างละ 2 ใบ มาใช้ในการทำงานของห้องคอน
    // ถ้าไม่มีใบรับ กับใบเบิกสี่ใบนี้โลจิคการทำงานของห้องคอนจะไม่สามารถทำงานได้
    fun execute() {
        // สร้างใบรับ และใบเบิก อย่างละใบ
        val token = cryptoService.encrypt("${adminUsername}|${LocalDateTime.now().format(LAST_ACTIVE_FORMAT)}")
        val authResult = wmsService.login(token)
        if (authResult == null) {
            logger.warn("The call to WMS's login return with null response...which is odd")
            return
        }

        val sessionId = authResult.first

        val pickingOrderData = createGoodMovementData(GoodMovementType.PICKING_ORDER, CONDITION_ROOM_DEPARTMENT_ID, "ห้อง Condition")
        var result = wmsService.createGoodMovement(sessionId, pickingOrderData)
        if (result is ResultOf.Failure) {
            logger.warn("Failed to create incoming picking order for month: ${dateTimeUtil.getYearMonthPrefix()}")
        }
        result = wmsService.createGoodMovement(sessionId, pickingOrderData)
        if (result is ResultOf.Failure) {
            logger.warn("Failed to create outgoing picking order for month: ${dateTimeUtil.getYearMonthPrefix()}")
        }

        val goodReceiptData = createGoodMovementData(GoodMovementType.GOODS_RECEIPT, CONDITION_ROOM_DEPARTMENT_ID, "ห้อง Condition")
        result = wmsService.createGoodMovement(sessionId, goodReceiptData)
        if (result is ResultOf.Failure) {
            logger.warn("Failed to create incoming good receipt for month: ${dateTimeUtil.getYearMonthPrefix()}")
        }
        result = wmsService.createGoodMovement(sessionId, goodReceiptData)
        if (result is ResultOf.Failure) {
            logger.warn("Failed to create outgoing good receipt for month: ${dateTimeUtil.getYearMonthPrefix()}")
        }


        var incomingPickingOrderId = -1
        var outgoingPickingOrderId = -1
        var incomingGoodReceiptId = -1
        var outgoingGoodReceiptId = -1
        Database.connect(dataSource)
        transaction {
            addLogger(ExposedInfoLogger)

            val daos = GoodMovementDao.find { GoodMovement.userId eq adminUserId.toInt() }
                .orderBy(GoodMovement.id to SortOrder.DESC)
                .limit(4)

            incomingPickingOrderId = daos.elementAt(0).id.value
            outgoingPickingOrderId = daos.elementAt(1).id.value
            incomingGoodReceiptId = daos.elementAt(2).id.value
            outgoingGoodReceiptId = daos.elementAt(3).id.value

            val now = LocalDateTime.now()
            val yearMonthPrefix = dateTimeUtil.getYearMonthPrefix()
            Config.insertAndGetId {
                it[name] = CONDITION_ROOM_INCOMING_PICK_PREFIX + yearMonthPrefix
                it[valueInt] = incomingPickingOrderId
                it[createdAt] = now
            }
            Config.insertAndGetId {
                it[name] = CONDITION_ROOM_OUTGOING_PICK_PREFIX + yearMonthPrefix
                it[valueInt] = outgoingPickingOrderId
                it[createdAt] = now
            }
            Config.insertAndGetId {
                it[name] = CONDITION_ROOM_INCOMING_RECEIVE_PREFIX + yearMonthPrefix
                it[valueInt] = incomingGoodReceiptId
                it[createdAt] = now
            }
            Config.insertAndGetId {
                it[name] = CONDITION_ROOM_OUTGOING_RECEIVE_PREFIX + yearMonthPrefix
                it[valueInt] = outgoingGoodReceiptId
                it[createdAt] = now
            }
        }

        if (incomingPickingOrderId > 0) {
            wmsService.approveGoodMovement(sessionId, incomingPickingOrderId)
        }
        if (outgoingPickingOrderId > 0) {
            wmsService.approveGoodMovement(sessionId, outgoingPickingOrderId)
        }
    }

    private fun createGoodMovementData(type: GoodMovementType, departmentId: Int, departmentName: String): GoodMovementData {
        return GoodMovementData(
            id = 0,
            type = type,
            departmentId = departmentId,
            department = departmentName,
            productionDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            supplierId = CHAMPACA_SUPPLIER_ID,
            supplier = "จัมปาก้า",
            productType = "COMPONENT",
            code = "",
            processType = null,
            processTypeId = null,
            manufacturingLine = null,
            manufacturingLineId = null,
            orderNo = null,
            jobNo = null,
            poNo = null,
            invoiceNo = null,
            lotNo = null,
            createdBy = "",
            approvedBy = null,
            remark = "ใช้ในการรับเบิกของเข้าออกห้อง Condition",
            extraAttributes = null,
        )
    }
}