package com.example.todolist.plugins

import com.example.todolist.utils.JwtUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*

// Ключ для хранения userId в атрибутах запроса
val UserIdKey = AttributeKey<String>("userId")

/**
 * Ktor-плагин для проверки JWT токена.
 * Применяется к защищённым маршрутам.
 *
 * Если токен валидный — userId сохраняется в call.attributes[UserIdKey]
 * Если токен невалидный — возвращается 401 Unauthorized
 */
val JwtAuth = createRouteScopedPlugin("JwtAuth") {
    onCall { call ->
        val authHeader = call.request.header(HttpHeaders.Authorization)

        // Проверяем наличие заголовка
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Missing or invalid Authorization header. Expected: Bearer <token>")
            )
            return@onCall
        }

        // Извлекаем токен
        val token = authHeader.removePrefix("Bearer ").trim()

        // Валидируем токен
        val userId = JwtUtils.validateToken(token)
        if (userId == null) {
            call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Invalid or expired token")
            )
            return@onCall
        }

        // Сохраняем userId в атрибутах запроса (на будущее)
        call.attributes.put(UserIdKey, userId)
    }
}