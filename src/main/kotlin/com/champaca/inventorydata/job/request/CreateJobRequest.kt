package com.champaca.inventorydata.job.request

data class CreateJobRequest(
    val jobNo: String,
    val orderNo: String?,
    val invoiceNo: String?,
    val lotNo: String?,
    val fsc: String?,
    val productionDate: String,
    val endDate: String?
)