package com.example.todolist.plugins

import com.example.todolist.data.repository.UserRepository
import com.example.todolist.utils.JwtUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(userRepository: UserRepository) {

    post("/auth/register") {
        try {
            val request = call.receive<RegisterRequest>()

            if (request.email.isBlank() || request.password.length < 6) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Логин и пароль обязательны. Минимум 6 символов")
                )
                return@post
            }

            val existingUser = userRepository.findUserByEmail(request.email)
            if (existingUser != null) {
                call.respond(
                    HttpStatusCode.Conflict,
                    ErrorResponse("Пользователь с таким логином уже существует")
                )
                return@post
            }

            val newUser = userRepository.createUser(
                email = request.email,
                displayName = request.displayName,
                password = request.password
            )

            call.respond(
                HttpStatusCode.Created,
                AuthResponse(newUser.id, newUser.email, newUser.displayName)
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Ошибка регистрации: ${e.message}")
            )
        }
    }

    post("/auth/login") {
        try {
            val request = call.receive<LoginRequest>()

            val user = userRepository.findUserByEmail(request.email)
            if (user == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Пользователь не найден")
                )
                return@post
            }

            val isValidPassword = userRepository.verifyPassword(
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
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Ошибка входа: ${e.message}")
            )
        }
    }
}