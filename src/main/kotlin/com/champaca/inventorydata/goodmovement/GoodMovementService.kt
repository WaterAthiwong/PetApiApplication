package com.champaca.inventorydata.goodmovement

import com.champaca.inventorydata.databasetable.dao.*
import com.champaca.inventorydata.goodmovement.model.GoodMovementData
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class GoodMovementService {

    val PRODUCTION_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getGoodMovementData(dao: GoodMovementDao): GoodMovementData {
        val manufacturingLine = ManufacturingLineDao.findById(dao.manufacturingLineId ?: -1)
        val processType = ProcessTypeDao.findById(manufacturingLine?.processTypeId ?: -1)
        val supplier = SupplierDao.findById(dao.supplierId ?: -1)
        val department = DepartmentDao.findById(dao.departmentId.value)!!
        val approvedUserDao = UserDao.findById(dao.approveUserId ?: -1)
        val approvedByUser = if (approvedUserDao != null) {
            "${approvedUserDao.firstname} ${approvedUserDao.lastname}"
        } else {
            ""
        }
        val createdByUser = UserDao.findById(dao.userId)!!
        val createdBy = "${createdByUser.firstname} ${createdByUser.lastname}"
        return GoodMovementData(
            id = dao.id.value,
            code = dao.code,
            type = GoodMovementType.fromString(dao.type),
            processType = processType?.name,
            processTypeId = processType?.id?.value,
            manufacturingLineId = dao.manufacturingLineId,
            manufacturingLine = manufacturingLine?.name,
            departmentId = dao.departmentId.value,
            department = department.name,
            productionDate = PRODUCTION_DATE_FORMAT.format(dao.productionDate),
            orderNo = dao.orderNo,
            jobNo = dao.jobNo,
            poNo = dao.poNo,
            invoiceNo = dao.invoiceNo,
            lotNo = dao.lotNo,
            supplierId = dao.supplierId,
            supplier = supplier?.name,
            createdBy = createdBy,
            approvedBy = approvedByUser,
            remark = dao.remark,
            extraAttributes = dao.extraAttributes,
            productType = dao.productType
        )
    }
}