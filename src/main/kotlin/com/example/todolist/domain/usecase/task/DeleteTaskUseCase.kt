package com.example.todolist.domain.usecase.task

import com.example.todolist.domain.repository.TaskRepository

class DeleteTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String, userId: String): Boolean {
        return taskRepository.deleteTask(taskId, userId)
    }
}