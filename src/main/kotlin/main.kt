package com.example.todolist

import com.example.todolist.data.db.DatabaseFactory
import com.example.todolist.data.repository.PostRepositoryImpl
import com.example.todolist.data.repository.TaskRepositoryImpl
import com.example.todolist.data.repository.UserRepository
import com.example.todolist.domain.repository.AuthRepository
import com.example.todolist.plugins.configureRouting
import com.example.todolist.plugins.configureStatusPages
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import kotlinx.serialization.json.Json

fun main() {
    DatabaseFactory.init()
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(CallLogging)

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
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
    }

    configureStatusPages()

    val authRepository: AuthRepository = UserRepository()
    val taskRepository = TaskRepositoryImpl()
    val postRepository = PostRepositoryImpl()

    configureRouting(taskRepository, authRepository, postRepository)
}
