package com.example.todolist.plugins

import com.example.todolist.domain.repository.TaskRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.taskRoutes(taskRepository: TaskRepository) {

    get("/tasks") {
        val userId = call.parameters["userId"]
        if (userId == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("userId is required"))
            return@get
        }

        try {
            val tasks = taskRepository.getTasks(userId)
            call.respond(HttpStatusCode.OK, tasks)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(e.message ?: "Unknown error")
            )
        }
    }

    post("/tasks") {
        val userId = call.parameters["userId"] ?: run {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("userId is required"))
            return@post
        }

        try {
            val request = call.receive<CreateTaskRequest>()
            if (request.title.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Title cannot be empty")
                )
                return@post
            }

            val newTask = taskRepository.createTask(
                userId,
                request.title.trim(),
                request.priority,
                request.dueDate
            )
            call.respond(HttpStatusCode.Created, newTask)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(e.message ?: "Failed to create task")
            )
        }
    }

    delete("/tasks/{id}") {
        val taskId = call.parameters["id"] ?: run {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Task ID is required"))
            return@delete
        }
        val userId = call.parameters["userId"] ?: run {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("userId is required"))
            return@delete
        }

        try {
            val deleted = taskRepository.deleteTask(taskId, userId)
            if (deleted) {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Task deleted"))
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Task not found"))
            }
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(e.message ?: "Failed to delete task")
            )
        }
    }

    put("/tasks/{id}") {
        val taskId = call.parameters["id"] ?: run {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Task ID is required"))
            return@put
        }
        val userId = call.parameters["userId"] ?: run {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("userId is required"))
            return@put
        }

        try {
            val request = call.receive<UpdateTaskRequest>()
            val updated = taskRepository.updateTask(
                taskId,
                userId,
                request.title,
                request.isDone,
                request.priority,
                request.dueDate
            )
            if (updated) call.respond(HttpStatusCode.OK)
            else call.respond(HttpStatusCode.NotFound)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}