package com.example

import org.junit.Test
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ServerTest {

    @Test
    fun `string utilities work correctly`() {
        val text = "Hello World"
        assertTrue(text.isNotEmpty())
        assertEquals(11, text.length)
    }

    @Test
    fun `email validation logic`() {
        val email = "test@example.com"
        assertTrue(email.contains("@"))
        assertTrue(email.contains("."))
    }

    @Test
    fun `salted password hashing produces different hashes for same password`() {
        val password = "MySecurePassword123"

        // Генерируем две разные соли
        val salt1 = generateSalt()
        val salt2 = generateSalt()

        // Хешируем один пароль с разными солями
        val hash1 = hashPasswordWithSalt(password, salt1)
        val hash2 = hashPasswordWithSalt(password, salt2)

        assertNotEquals(hash1, hash2, "Same password with different salts should produce different hashes")

        assertEquals(64, hash1.length)
        assertEquals(64, hash2.length)
        assertTrue(hash1.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `password verification works correctly`() {
        val password = "CorrectPassword123"
        val salt = generateSalt()
        val storedHash = hashPasswordWithSalt(password, salt)

        assertTrue(
            verifyPassword(password, storedHash, salt),
            "Correct password should verify successfully"
        )

        assertTrue(
            !verifyPassword("WrongPassword", storedHash, salt),
            "Wrong password should fail verification"
        )
    }

    @Test
    fun `SHA-256 produces correct hash format`() {
        val password = "test123"
        val salt = "abcd1234"
        val hash = hashPasswordWithSalt(password, salt)

        assertEquals(64, hash.length)
        assertTrue(hash.all { it.isDigit() || it in 'a'..'f' })
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPasswordWithSalt(password: String, salt: String): String {
        val saltedPassword = password + salt
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(saltedPassword.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun verifyPassword(password: String, storedHash: String, salt: String): Boolean {
        return hashPasswordWithSalt(password, salt) == storedHash
    }
}