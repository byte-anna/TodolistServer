package com.example.todolist.data.repository

import com.example.todolist.data.db.DatabaseFactory
import com.example.todolist.data.db.UsersTable
import com.example.todolist.domain.model.User
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.UUID

class UserRepository {

    fun findUserByEmail(email: String): User? {
        val result = DatabaseFactory.dbQuery {
            UsersTable.select { UsersTable.email eq email }.singleOrNull()
        }
        return result?.let { row ->
            User(
                id = row[UsersTable.id],
                email = row[UsersTable.email],
                displayName = row[UsersTable.displayName],  // ✅ ДОБАВИЛИ ЭТО!
                passwordHash = row[UsersTable.passwordHash],
                createdAt = row[UsersTable.createdAt].toString()
            )
        }
    }

    fun createUser(email: String, displayName: String?, passwordHash: String): User {
        val userId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        DatabaseFactory.dbQuery {
            UsersTable.insert {
                it[id] = userId
                it[this.email] = email
                it[this.displayName] = displayName  // ✅ Эта строка должна быть
                it[this.passwordHash] = passwordHash
                it[createdAt] = now
            }
        }

        return User(userId, email, displayName, passwordHash, now.toString())
    }

    fun hashPassword(password: String): String {
        return java.security.MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}