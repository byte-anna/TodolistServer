package com.example.todolist.domain.repository

import com.example.todolist.domain.model.User

interface AuthRepository {
    fun findUserByEmail(email: String): User?
    fun createUser(email: String, displayName: String?, password: String): User
    fun authenticate(email: String, password: String): User?
}
