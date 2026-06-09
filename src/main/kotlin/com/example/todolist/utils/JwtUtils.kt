package com.example.todolist.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.util.Date

object JwtUtils {
    private val secret: String by lazy {
        System.getenv("JWT_SECRET")
            ?.takeIf { it.isNotBlank() }
            ?: System.getProperty("JWT_SECRET")
                ?.takeIf { it.isNotBlank() }
            ?: error("JWT_SECRET is required")
    }

    private const val EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000

    private val algorithm: Algorithm by lazy { Algorithm.HMAC256(secret) }

    private val verifier by lazy {
        JWT.require(algorithm).build()
    }

    fun generateToken(userId: String, email: String): String {
        return JWT.create()
            .withSubject(userId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + EXPIRATION_MS))
            .sign(algorithm)
    }

    fun validateToken(token: String): String? {
        return try {
            val decoded = verifier.verify(token)
            decoded.subject
        } catch (_: JWTVerificationException) {
            null
        }
    }
}
