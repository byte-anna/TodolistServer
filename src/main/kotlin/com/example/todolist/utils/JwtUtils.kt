package com.example.todolist.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.util.*

object JwtUtils {
    private val secret = System.getenv("JWT_SECRET") ?: "your-secret-key-change-in-prod"
    private const val EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000 // 7 дней

    private val algorithm = Algorithm.HMAC256(secret)

    private val verifier = JWT.require(algorithm)
        .build()

    fun generateToken(userId: String, email: String): String {
        return JWT.create()
            .withSubject(userId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + EXPIRATION_MS))
            .sign(algorithm)
    }

    /**
     * Проверяет JWT токен и возвращает userId, если токен валидный.
     * Возвращает null, если токен невалидный или истёк.
     */
    fun validateToken(token: String): String? {
        return try {
            val decoded = verifier.verify(token)
            decoded.subject  // userId
        } catch (e: JWTVerificationException) {
            null
        }
    }
}