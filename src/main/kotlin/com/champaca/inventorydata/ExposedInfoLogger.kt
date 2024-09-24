package com.champaca.inventorydata

import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.slf4j.LoggerFactory

object ExposedInfoLogger: SqlLogger {
    private val logger = LoggerFactory.getLogger("Exposed")!!

    override fun log(context: StatementContext, transaction: Transaction) {
        if (logger.isInfoEnabled) {
            logger.info(context.expandArgs(TransactionManager.current()))
        }
    }
}