package com.example.todolist

import com.example.todolist.data.db.DatabaseFactory
import com.example.todolist.data.repository.FolderRepositoryImpl
import com.example.todolist.data.repository.TaskRepositoryImpl
import com.example.todolist.data.repository.UserRepository  // ✅ Правильный импорт
import com.example.todolist.plugins.configureRouting
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun main() {
    DatabaseFactory.init()
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(CORS) {
            anyHost()
            allowMethod(io.ktor.http.HttpMethod.Get)
            allowMethod(io.ktor.http.HttpMethod.Post)
            allowMethod(io.ktor.http.HttpMethod.Put)
            allowMethod(io.ktor.http.HttpMethod.Delete)
            allowHeader(io.ktor.http.HttpHeaders.ContentType)
        }

        // ✅ Создаём репозитории (правильные имена!)
        val userRepository = UserRepository()
        val taskRepository = TaskRepositoryImpl()
        val folderRepository = FolderRepositoryImpl()

        // ✅ Передаём все репозитории
        configureRouting(taskRepository, userRepository, folderRepository)
    }.start(wait = true)
}