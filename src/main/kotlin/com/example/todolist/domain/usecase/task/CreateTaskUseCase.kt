package com.example.todolist.domain.usecase.task

import com.example.todolist.domain.model.Task
import com.example.todolist.domain.repository.TaskRepository

class CreateTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(
        userId: String,
        title: String,
        priority: Int,
        dueDate: String?
    ): Task {
        return taskRepository.createTask(userId, title, priority, dueDate)
    }
}