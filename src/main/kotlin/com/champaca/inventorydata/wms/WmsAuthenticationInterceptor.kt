package com.champaca.inventorydata.wms

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.common.ChampacaConstant.USERNAME
import com.champaca.inventorydata.common.ChampacaConstant.USER_ID
import com.champaca.inventorydata.databasetable.UserHasGroup
import com.champaca.inventorydata.databasetable.dao.UserHasGroupDao
import com.champaca.inventorydata.masterdata.user.UserRepository
import com.champaca.inventorydata.wms.WmsService.Companion.WMS_SESSION
import com.champaca.inventorydata.wms.responsemodel.UserAuthJsonResponse
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.sql.DataSource

@Component
class WmsAuthenticationInterceptor(
    val cryptoService: CryptoService,
    val wmsService: WmsService,
    val dataSource: DataSource,
    val userRepository: UserRepository
): HandlerInterceptor {

    val logger = LoggerFactory.getLogger(WmsAuthenticationInterceptor::class.java)

    companion object {
        const val SESSION = "Session"
        const val ACTIVE_MINUTES = 60 * 8 // 8 Hours
        val LAST_ACTIVE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")
        val SESSION_FORMAT = "[a-z0-9]{32}\\|\\d{1,4}\\|\\d{8} \\d{2}:\\d{2}:\\d{2}".toRegex()
        val WMS_GROUPS = listOf(3, 4)
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.method.equals("OPTIONS")) {
            // if the request method is OPTIONS, it is likely that this is a CORS-testing request which is a "preflight"
            // request sending out to check about CORS handling. When this preflight request reaches this interceptor
            // we need to return true to make the request success,
            return true
        }

        val encryptedSession = request.getHeader(SESSION)
        val username = request.getHeader(USERNAME)

        if (encryptedSession != null) {
            val session = cryptoService.decode(encryptedSession) // the decrypted session should be like this c95a1efa200c22b91df53551c8e87f44|10|20231111 11:44:03
            if(isValidSession(session)) {
                // If the user already has a session, and it is still in active time range
                logger.debug("The user has an existing-active session. So reuse it.")
                val values = session.split("|")
                val wmsSession = values[0]
                val userId = values[1]
                request.setAttribute(WMS_SESSION, wmsSession)
                request.setAttribute(USER_ID, userId)
                response.addHeader(SESSION, encodeSessionData(wmsSession, userId))
                response.addHeader("Access-Control-Expose-Headers", "Session")
                return true
            }
        }

        val token = cryptoService.encrypt("${username}|${LocalDateTime.now().format(LAST_ACTIVE_FORMAT)}")

        // Call WMS to login (authenticate)
        logger.debug("Call WMS's login for a new session")
        val authResult = wmsService.login(token)
        if (authResult == null) {
            logger.debug("The call to WMS's login return with null response...which is odd")
            return false
        }

        val body = authResult.second
        val wmsSession = authResult.first
        if (body.header.code != 1) {
            createErrorResponse(response, body)
            return false
        }

        var userId = ""
        var isAuthorized = false
        Database.connect(dataSource)
        transaction {
            addLogger(ExposedInfoLogger)
            val user = userRepository.findByUsername(username)!!
            userId = user.id.value.toString()
            isAuthorized = userHasWmsPrivilege(userId)
        }

        if (!isAuthorized) {
            createErrorResponse(response, "User is not authorized to access WMS", "WMS_AUTHORIZATION_FAILED")
            return false
        }

        request.setAttribute(WMS_SESSION, wmsSession)
        request.setAttribute(USER_ID, userId)
        response.addHeader(SESSION, encodeSessionData(wmsSession, userId))
        response.addHeader("Access-Control-Expose-Headers", "Session")
        return true
    }

    private fun userHasWmsPrivilege(userId: String): Boolean {
        return UserHasGroupDao.find { (UserHasGroup.userId eq userId.toInt()) and (UserHasGroup.groupId inList WMS_GROUPS) }
            .toList().isNotEmpty()
    }

    private fun encodeSessionData(wmsSession: String, userId: String): String {
        val data = "$wmsSession|$userId|${LocalDateTime.now().format(LAST_ACTIVE_FORMAT)}"
        return cryptoService.encode(data)
    }

    private fun isValidSession(session: String): Boolean {
        if (!session.matches(SESSION_FORMAT)) {
            return false
        }

        val lastActiveStr = session.split("|")[2]
        val lastActive = LocalDateTime.parse(lastActiveStr, LAST_ACTIVE_FORMAT)
        val current = LocalDateTime.now()
        return Duration.between(lastActive, current).toMinutes() < ACTIVE_MINUTES
    }

    private fun createErrorResponse(response: HttpServletResponse, authResult: UserAuthJsonResponse) {
        val message = authResult.header.message
        response.apply {
            status = 400
            contentType = "application/json"
            writer.use { writer ->
                writer.write("{\"errorMessage\": \"${message}\", \"errorType\": \"WMS_AUTHENTICATION_FAILED\"}")
                writer.flush()
            }
        }
    }

    private fun createErrorResponse(response: HttpServletResponse, message: String, errorType: String) {
        response.apply {
            status = 400
            contentType = "application/json"
            writer.use { writer ->
                writer.write("{\"errorMessage\": \"${message}\", \"errorType\": \"${errorType}\"}")
                writer.flush()
            }
        }
    }
}