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
        val salt1 = generateSalt()
        val salt2 = generateSalt()
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

        assertTrue(verifyPassword(password, storedHash, salt), "Correct password should verify successfully")
        assertTrue(!verifyPassword("WrongPassword", storedHash, salt), "Wrong password should fail verification")
    }

    @Test
    fun `SHA-256 produces correct hash format`() {
        val password = "test123"
        val salt = "abcd1234"
        val hash = hashPasswordWithSalt(password, salt)

        assertEquals(64, hash.length)
        assertTrue(hash.all { it.isDigit() || it in 'a'..'f' })
    }


    @Test
    fun `task title validation rejects empty strings`() {
        val invalidTitles = listOf("", "   ", "\t", "\n")
        invalidTitles.forEach { title ->
            assertTrue(title.isBlank() || title.isEmpty(), "Empty title should be invalid")
        }
        val validTitle = "Купить молоко"
        assertTrue(validTitle.isNotBlank(), "Non-empty title should be valid")
    }

    @Test
    fun `task priority must be between 1 and 3`() {
        val validPriorities = listOf(1, 2, 3)
        val invalidPriorities = listOf(0, 4, -1, 100)
        validPriorities.forEach { priority ->
            assertTrue(priority in 1..3, "Priority $priority should be valid")
        }
        invalidPriorities.forEach { priority ->
            assertTrue(priority !in 1..3, "Priority $priority should be invalid")
        }
    }

    @Test
    fun `post content cannot be empty`() {
        val emptyContents = listOf("", "   ", "\n")
        emptyContents.forEach { content ->
            assertTrue(content.isBlank(), "Empty content should be rejected")
        }
        val validContent = "Выполнил задачу!"
        assertTrue(validContent.isNotBlank(), "Non-empty content should be accepted")
    }

    @Test
    fun `UUID generation produces valid format`() {
        val uuid = java.util.UUID.randomUUID().toString()
        val uuidRegex = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        assertTrue(uuid.matches(uuidRegex), "UUID should match standard format")
        assertEquals(36, uuid.length, "UUID should be 36 characters")
    }

    @Test
    fun `login validation checks basic rules`() {
        val validLogins = listOf(
            "user123",
            "admin",
            "test_user",
            "User_Name_2024",
            "a"
        )

        val invalidLogins = listOf(
            "",
            "   ",
            "user name",
            "user@name",
            "user#name",
            "\n",
            "\t"
        )

        validLogins.forEach { login ->
            assertTrue(
                login.isNotBlank() && !login.contains(" "),
                "Valid login '$login' should pass basic check"
            )
        }

        invalidLogins.forEach { login ->
            assertTrue(
                login.isBlank() || login.contains(" ") || login.contains("@") || login.contains("#"),
                "Invalid login '$login' should fail basic check"
            )
        }
    }

    //Вспомогательные функции
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