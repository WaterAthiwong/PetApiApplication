package com.champaca.inventorydata.log.response

data class ValidateForestryFileResponse(
    val allRowsValid: Boolean,
    val wrongFormatRefCodes: List<InvalidRefCode>? = null,
    val existingRefCodes: List<InvalidRefCode>? = null,
    val duplicatedRefCodes: List<DuplicatedRefCode>? = null,
    val nonExistingMatCodes: List<NonExistingMatCode>? =null,
    val refCodesExistOnlyInFile: List<InvalidRefCode>? = null,
    val refCodesExistOnlyInBarcodeScanner: List<String>?
)

data class InvalidRefCode(
    val refCode: String,
    val position: RowPosition
)

data class DuplicatedRefCode(
    val refCode: String,
    val positions: List<RowPosition>
)

data class NonExistingMatCode(
    val matCode: String,
    val position: RowPosition
)

data class RowPosition(
    val tabName: String,
    val order: Int,
    val logNo: String
)
