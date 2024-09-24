package com.champaca.inventorydata.log.request

data class ValidateTransfromForestryFileParams(
    val fsc: Boolean,
    val scannedRefCodes: List<String>?,
    val supplier: Int,
    val forestryBook: String?,
    val forestryBookNo: String?
) {
    fun toValidateForestryFileParams(): ValidateForestryFileParams {
        return ValidateForestryFileParams(
            fsc = this.fsc,
            scannedRefCodes = this.scannedRefCodes,
            supplier = this.supplier
        )
    }
}

