package com.example.todolist.domain.usecase.post

import com.example.todolist.domain.model.Post
import com.example.todolist.domain.repository.PostRepository

class GetPostsUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(): List<Post> {
        return postRepository.getPosts()
    }
}