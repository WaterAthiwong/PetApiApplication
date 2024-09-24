package com.champaca.inventorydata.common

sealed class ResultOf<out T> {
    data class Success<out R>(val value: R): ResultOf<R>()
    data class Failure(
        val message: String? = "",
        val throwable: Throwable? = null
    ): ResultOf<Nothing>()
}