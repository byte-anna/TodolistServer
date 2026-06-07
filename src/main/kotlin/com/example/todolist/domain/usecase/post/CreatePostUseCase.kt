package com.example.todolist.domain.usecase.post

import com.example.todolist.domain.model.Post
import com.example.todolist.domain.repository.PostRepository

class CreatePostUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(userId: String, content: String, taskId: String?): Post {
        return postRepository.createPost(userId, content, taskId)
    }
}