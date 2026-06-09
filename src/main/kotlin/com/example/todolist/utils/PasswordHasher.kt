package com.example.todolist.utils

import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest
import java.security.SecureRandom

object PasswordHasher {

    private const val BCRYPT_ROUNDS = 12

    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS, SecureRandom()))
    }

    fun verifyPassword(password: String, hashedPassword: String, salt: String): Boolean {
        return when {
            hashedPassword.startsWith("$2a$") ||
                hashedPassword.startsWith("$2b$") ||
                hashedPassword.startsWith("$2y$") -> BCrypt.checkpw(password, hashedPassword)

            salt.isNotBlank() -> hashLegacyPassword(password, salt) == hashedPassword
            else -> false
        }
    }

    private fun hashLegacyPassword(password: String, salt: String): String {
        val saltedPassword = password + salt
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(saltedPassword.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
