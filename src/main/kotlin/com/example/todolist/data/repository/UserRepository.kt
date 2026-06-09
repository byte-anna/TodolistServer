package com.example.todolist.data.repository

import com.example.todolist.data.db.DatabaseFactory
import com.example.todolist.data.db.UsersTable
import com.example.todolist.domain.model.User
import com.example.todolist.domain.repository.AuthRepository
import com.example.todolist.utils.PasswordHasher
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.time.LocalDateTime
import java.util.UUID

class UserRepository : AuthRepository {

    override fun findUserByEmail(email: String): User? {
        return findStoredUserByEmail(email)?.toPublicUser()
    }

    override fun createUser(email: String, displayName: String?, password: String): User {
        val userId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        val normalizedEmail = normalizeEmail(email)
        val passwordHash = PasswordHasher.hashPassword(password)
        val salt = ""

        DatabaseFactory.dbQuery {
            UsersTable.insert {
                it[id] = userId
                it[this.email] = normalizedEmail
                it[this.displayName] = displayName
                it[this.passwordHash] = passwordHash
                it[this.salt] = salt
                it[createdAt] = now
            }
        }

        return User(
            id = userId,
            email = normalizedEmail,
            displayName = displayName,
            createdAt = now.toString()
        )
    }

    override fun authenticate(email: String, password: String): User? {
        val storedUser = findStoredUserByEmail(email) ?: return null
        val isValid = PasswordHasher.verifyPassword(
            password = password,
            hashedPassword = storedUser.passwordHash,
            salt = storedUser.salt
        )

        return if (isValid) storedUser.toPublicUser() else null
    }

    private fun findStoredUserByEmail(email: String): StoredUser? {
        val normalizedEmail = normalizeEmail(email)
        val result = DatabaseFactory.dbQuery {
            UsersTable.select { UsersTable.email eq normalizedEmail }.singleOrNull()
        }

        return result?.let { row ->
            StoredUser(
                id = row[UsersTable.id],
                email = row[UsersTable.email],
                displayName = row[UsersTable.displayName],
                passwordHash = row[UsersTable.passwordHash],
                salt = row[UsersTable.salt],
                createdAt = row[UsersTable.createdAt].toString()
            )
        }
    }

    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase()
    }

    private data class StoredUser(
        val id: String,
        val email: String,
        val displayName: String?,
        val passwordHash: String,
        val salt: String,
        val createdAt: String
    ) {
        fun toPublicUser(): User {
            return User(
                id = id,
                email = email,
                displayName = displayName,
                createdAt = createdAt
            )
        }
    }
}
