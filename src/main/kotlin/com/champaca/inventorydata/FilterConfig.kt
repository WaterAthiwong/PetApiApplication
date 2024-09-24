package com.champaca.inventorydata

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FilterConfig(
    val loggingFilter: LoggingFilter
) {

    @Bean
    fun customFilterRegistration(): FilterRegistrationBean<LoggingFilter> {
        val registration = FilterRegistrationBean<LoggingFilter>()
        registration.filter = loggingFilter
        registration.addUrlPatterns("/*") // Specify the URL patterns for the filter
        registration.order = 1 // Specify the order in which filters are executed
        return registration
    }
}