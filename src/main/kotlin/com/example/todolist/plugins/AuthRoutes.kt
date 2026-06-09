package com.example.todolist.plugins

import com.example.todolist.domain.repository.AuthRepository
import com.example.todolist.utils.JwtUtils
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.authRoutes(authRepository: AuthRepository) {

    post("/auth/register") {
        val request = call.receive<RegisterRequest>()

        if (request.email.isBlank() || request.password.length < 6) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Логин и пароль обязательны. Минимум 6 символов")
            )
            return@post
        }

        val existingUser = authRepository.findUserByEmail(request.email)
        if (existingUser != null) {
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse("Пользователь с таким логином уже существует")
            )
            return@post
        }

        val newUser = authRepository.createUser(
            email = request.email,
            displayName = request.displayName,
            password = request.password
        )

        call.respond(
            HttpStatusCode.Created,
            AuthResponse(newUser.id, newUser.email, newUser.displayName)
        )
    }

    post("/auth/login") {
        val request = call.receive<LoginRequest>()

        val user = authRepository.findUserByEmail(request.email)
        if (user == null) {
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse("Пользователь не найден")
            )
            return@post
        }

        val isValidPassword = authRepository.verifyPassword(
            password = request.password,
            passwordHash = user.passwordHash,
            salt = user.salt
        )

        if (!isValidPassword) {
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse("Неверный пароль")
            )
            return@post
        }

        val token = JwtUtils.generateToken(user.id, user.email)
        call.respond(
            HttpStatusCode.OK,
            AuthResponse(
                userId = user.id,
                email = user.email,
                displayName = user.displayName,
                token = token
            )
        )
    }
}
