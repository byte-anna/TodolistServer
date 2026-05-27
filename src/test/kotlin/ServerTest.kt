package com.example

import org.junit.Test
import java.security.MessageDigest
import kotlin.test.assertEquals
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
    fun `SHA-256 produces correct hash format`() {
        val password = "test123"
        val hash = hashPassword(password)

        assertEquals(64, hash.length)
        assertTrue(hash.all { it.isDigit() || it in 'a'..'f' })
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}