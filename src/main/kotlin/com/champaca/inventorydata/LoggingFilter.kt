package com.champaca.inventorydata

import com.champaca.inventorydata.common.ChampacaConstant
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import javax.servlet.*
import javax.servlet.http.HttpServletRequest

@Component
class LoggingFilter: Filter {

    val logger = LoggerFactory.getLogger(LoggingFilter::class.java)

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        try {
            if (request.method != "OPTIONS") {
                // Set the username (or any other data) in MDC
                val username = httpRequest.getHeader(ChampacaConstant.USERNAME)
                MDC.put("username", username)
                request.setAttribute("duration", LocalDateTime.now())
                logger.info("Start ${request.method} ${request.requestURI} >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
            }

            // Continue the request
            chain.doFilter(request, response)
        } finally {
            if (request.method != "OPTIONS") {
                logger.info("End ${request.method} ${request.requestURI} <<<< ${calculateDuration(request)} ms <<<<<<<<<<<<<<<<<<<<<<<<<<<<")
                // Clear MDC data to avoid memory leak
                MDC.clear()
            }
        }
    }

    private fun calculateDuration(request: HttpServletRequest): Long {
        val start = request.getAttribute("duration") as LocalDateTime
        return Duration.between(start, LocalDateTime.now()).toMillis()
    }

    override fun destroy() {}
}