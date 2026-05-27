package com.example.todolist.utils

import java.security.MessageDigest

object PasswordHasher {

    /**
     * Хеширует пароль с использованием SHA-256 и соли
     */
    fun hashPassword(password: String, salt: String = generateSalt()): String {
        val saltedPassword = password + salt
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(saltedPassword.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Проверяет, соответствует ли пароль хешу
     */
    fun verifyPassword(password: String, hashedPassword: String, salt: String): Boolean {
        return hashPassword(password, salt) == hashedPassword
    }

    /**
     * Генерирует случайную соль (16 байт в hex)
     */
    private fun generateSalt(): String {
        val random = java.security.SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}