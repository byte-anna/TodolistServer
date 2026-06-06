package com.example.todolist.plugins

import com.example.todolist.utils.JwtUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*

// Ключ для хранения userId в атрибутах запроса
val UserIdKey = AttributeKey<String>("userId")

/**
 * Ktor-плагин для извлечения пользователя из JWT.
 * Применяется к защищённым маршрутам.
 *
 * Если токен валидный — userId сохраняется в call.attributes[UserIdKey].
 * За обязательную проверку отвечает requireAuthenticatedUserId() в обработчиках маршрутов.
 */
val JwtAuth = createRouteScopedPlugin("JwtAuth") {
    onCall { call ->
        call.extractUserIdFromBearerToken()?.let { userId ->
            call.attributes.put(UserIdKey, userId)
        }
    }
}

/**
 * Возвращает userId из валидного JWT и сохраняет его в атрибутах запроса.
 *
 * Важно: защищённые маршруты должны использовать именно это значение, а не userId
 * из query/path/body, потому что значения от клиента можно подменить.
 */
suspend fun ApplicationCall.requireAuthenticatedUserId(): String? {
    if (attributes.contains(UserIdKey)) {
        return attributes[UserIdKey]
    }

    val authHeader = request.header(HttpHeaders.Authorization)

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        respond(
            HttpStatusCode.Unauthorized,
            mapOf("error" to "Missing or invalid Authorization header. Expected: Bearer <token>")
        )
        return null
    }

    val userId = extractUserIdFromBearerToken()

    if (userId.isNullOrBlank()) {
        respond(
            HttpStatusCode.Unauthorized,
            mapOf("error" to "Invalid or expired token")
        )
        return null
    }

    attributes.put(UserIdKey, userId)
    return userId
}

private fun ApplicationCall.extractUserIdFromBearerToken(): String? {
    val authHeader = request.header(HttpHeaders.Authorization)
        ?: return null

    if (!authHeader.startsWith("Bearer ")) {
        return null
    }

    val token = authHeader.removePrefix("Bearer ").trim()
    return JwtUtils.validateToken(token)
}
