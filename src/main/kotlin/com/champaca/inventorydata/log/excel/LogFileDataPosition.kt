package com.champaca.inventorydata.log.excel

interface LogFileDataPosition {

    /**
     * The column of ลำดับที่.
     */
    val orderColumn: Char

    /**
     * The column of log species/
     */
    val speciesColumn: Char

    /**
     * The column of log length.
     */
    val lengthColumn: Char

    /**
     * The column of log circumference (ความโต)
     */
    val circumferenceColumn: Char

    /**
     * The column of เลขเขียง
     */
    val logNoColumn: Char

    /**
     * The column of log's volumn (in cubic meter)
     */
    val volumnM3Column: Char

    /**
     * The column of barcode.
     */
    val barcodeColumn: Char

    /**
     * The zero-based row where the data starts.
     */
    val startRow: Int

    /**
     * The name of the last column that has value.
     */
    val lastColumn: Char
}