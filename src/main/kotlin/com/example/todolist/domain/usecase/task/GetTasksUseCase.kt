package com.example.todolist.domain.usecase.task

import com.example.todolist.domain.model.Task
import com.example.todolist.domain.repository.TaskRepository

class GetTasksUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(userId: String): List<Task> {
        return taskRepository.getTasks(userId)
    }
}