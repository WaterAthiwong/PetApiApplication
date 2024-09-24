package com.champaca.inventorydata

import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.statements.expandArgs
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.slf4j.Logger

class ExposedLogger(
    val logger: Logger
): SqlLogger {
    override fun log(context: org.jetbrains.exposed.sql.statements.StatementContext, transaction: org.jetbrains.exposed.sql.Transaction) {
        if (logger.isInfoEnabled) {
            logger.info(context.expandArgs(TransactionManager.current()))
        }
    }
}