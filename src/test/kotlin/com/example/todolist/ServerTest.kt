package com.example.todolist

import com.example.todolist.data.db.DatabaseFactory
import com.example.todolist.plugins.AuthResponse
import com.example.todolist.plugins.CreatePostRequest
import com.example.todolist.plugins.CreateTaskRequest
import com.example.todolist.plugins.LoginRequest
import com.example.todolist.plugins.RegisterRequest
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ServerTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            System.setProperty("JWT_SECRET", "test-jwt-secret")
            DatabaseFactory.initForTests()
        }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private suspend fun registerAndLogin(
        client: io.ktor.client.HttpClient,
        email: String = "user_${System.currentTimeMillis()}@mail.com",
        password: String = "password123"
    ): AuthResponse {
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, password))
        }

        return client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body()
    }

    private fun HttpRequestBuilder.bearerAuth(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    @Test
    fun `POST auth register should create user and return 201`() = testApplication {
        application { module() }
        val client = jsonClient()

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("test_register_${System.currentTimeMillis()}@mail.com", "password123"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<AuthResponse>()
        assertNotNull(body.userId)
        assertTrue(body.userId.isNotEmpty())
    }

    @Test
    fun `POST auth register with duplicate email should return 409`() = testApplication {
        application { module() }
        val client = jsonClient()

        val email = "duplicate_${System.currentTimeMillis()}@mail.com"

        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, "password123"))
        }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, "password123"))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST auth register should normalize email and prevent duplicate with different case`() = testApplication {
        application { module() }
        val client = jsonClient()

        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("User_Test@Mail.com", "password123"))
        }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("user_test@mail.com", "password123"))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST auth login with valid credentials should return token`() = testApplication {
        application { module() }
        val client = jsonClient()

        val email = "login_test_${System.currentTimeMillis()}@mail.com"

        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, "password123"))
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, "password123"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<Map<String, String>>()
        assertNotNull(body["userId"])
        assertNotNull(body["token"])
    }

    @Test
    fun `POST auth login with wrong password should return 401`() = testApplication {
        application { module() }
        val client = jsonClient()

        val email = "wrong_pass_${System.currentTimeMillis()}@mail.com"

        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, "correct_password"))
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, "wrong_password"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST tasks should create task and return 201`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "task_test_${System.currentTimeMillis()}@mail.com")

        val response = client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Купить молоко", priority = 2))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val task = response.body<com.example.todolist.domain.model.Task>()
        assertEquals("Купить молоко", task.title)
        assertEquals(2, task.priority)
        assertEquals(false, task.isDone)
    }

    @Test
    fun `GET tasks should return user tasks`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "get_tasks_${System.currentTimeMillis()}@mail.com")

        client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Задача 1"))
        }
        client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Задача 2"))
        }

        val response = client.get("/tasks") {
            bearerAuth(auth.token!!)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val tasks = response.body<List<com.example.todolist.domain.model.Task>>()
        assertTrue(tasks.size >= 2)
    }

    @Test
    fun `GET tasks without token should return 401`() = testApplication {
        application { module() }
        val client = jsonClient()

        val response = client.get("/tasks")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST tasks with invalid dueDate should return 400`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "invalid_due_${System.currentTimeMillis()}@mail.com")

        val response = client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Task with bad date", dueDate = "tomorrow"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET posts should return list`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "posts_list_${System.currentTimeMillis()}@mail.com")

        val response = client.get("/posts") {
            bearerAuth(auth.token!!)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `POST posts should accept Android JSON with content and taskId`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "android_post_${System.currentTimeMillis()}@mail.com")
        val taskTitle = "task1_${System.currentTimeMillis()}"

        val task = client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(taskTitle))
        }.body<com.example.todolist.domain.model.Task>()

        val createPostResponse = client.post("/posts") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody("""{"content":"$taskTitle","taskId":"${task.id}"}""")
        }

        assertEquals(HttpStatusCode.Created, createPostResponse.status)
        val createdPost = createPostResponse.body<com.example.todolist.domain.model.Post>()
        assertEquals(taskTitle, createdPost.content)
        assertEquals(task.id, createdPost.taskId)

        val posts = client.get("/posts") {
            bearerAuth(auth.token!!)
        }.body<List<com.example.todolist.domain.model.Post>>()

        val publishedPost = posts.first { it.id == createdPost.id }
        assertEquals(taskTitle, publishedPost.content)
        assertEquals(task.id, publishedPost.taskId)
    }

    @Test
    fun `POST posts should create post`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "post_test_${System.currentTimeMillis()}@mail.com")

        val response = client.post("/posts") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreatePostRequest("Выполнил задачу!", null))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST post like with invalid token should return 401`() = testApplication {
        application { module() }
        val client = jsonClient()

        val response = client.post("/posts/some-id/like") {
            bearerAuth("broken-token")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
