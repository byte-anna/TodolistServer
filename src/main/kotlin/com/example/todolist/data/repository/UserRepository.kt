package com.example.todolist.data.repository

import com.example.todolist.data.db.DatabaseFactory
import com.example.todolist.data.db.UsersTable
import com.example.todolist.domain.model.User
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.UUID

class UserRepository {

    private fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, salt: String): String {
        val saltedPassword = password + salt
        return MessageDigest
            .getInstance("SHA-256")
            .digest(saltedPassword.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, passwordHash: String, salt: String): Boolean {
        return hashPassword(password, salt) == passwordHash
    }

    fun findUserByEmail(email: String): User? {
        val result = DatabaseFactory.dbQuery {
            UsersTable.select { UsersTable.email eq email }.singleOrNull()
        }
        return result?.let { row ->
            User(
                id = row[UsersTable.id],
                email = row[UsersTable.email],
                displayName = row[UsersTable.displayName],
                passwordHash = row[UsersTable.passwordHash],
                salt = row[UsersTable.salt],
                createdAt = row[UsersTable.createdAt].toString()
            )
        }
    }

    fun createUser(email: String, displayName: String?, password: String): User {
        val userId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        val salt = generateSalt()
        val passwordHash = hashPassword(password, salt)

        DatabaseFactory.dbQuery {
            UsersTable.insert {
                it[id] = userId
                it[this.email] = email
                it[this.displayName] = displayName
                it[this.passwordHash] = passwordHash
                it[this.salt] = salt
                it[createdAt] = now
            }
        }

        return User(userId, email, displayName, passwordHash, salt, now.toString())
    }
}