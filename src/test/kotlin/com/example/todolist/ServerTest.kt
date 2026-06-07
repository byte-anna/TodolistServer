package com.example.todolist

import com.example.todolist.data.db.DatabaseFactory
import com.example.todolist.plugins.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.*
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
            DatabaseFactory.initForTests()
        }
    }

    // Вспомогательная функция — создаёт клиент с JSON
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

        // Первая регистрация — успешна
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, "password123"))
        }

        // Вторая регистрация — конфликт
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, "password123"))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST auth login with valid credentials should return token`() = testApplication {
        application { module() }
        val client = jsonClient()

        val email = "login_test_${System.currentTimeMillis()}@mail.com"

        // Сначала регистрируем
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, "password123"))
        }

        // Теперь логинимся
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

        // Регистрируем
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, "correct_password"))
        }

        // Логин с неверным паролем
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, "wrong_password"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // ============ TASKS TESTS ============

    @Test
    fun `POST tasks should create task and return 201`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "task_test_${System.currentTimeMillis()}@mail.com")

        // Создаём задачу. userId в query намеренно не передаётся: сервер берёт пользователя из JWT.
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

        // Создаём 2 задачи
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

        // Получаем задачи
        val response = client.get("/tasks") {
            bearerAuth(auth.token!!)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val tasks = response.body<List<com.example.todolist.domain.model.Task>>()
        assertTrue(tasks.size >= 2)
    }

    // ============ POSTS TESTS ============

    @Test
    fun `GET posts should return list`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "posts_list_${System.currentTimeMillis()}@mail.com")

        val response = client.get("/posts") {
            bearerAuth(auth.token!!)
        }

        println("STATUS = ${response.status}")
        println("BODY = ${response.bodyAsText()}")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `POST posts should create post`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "post_test_${System.currentTimeMillis()}@mail.com")

        // Создаём пост. userId в body больше не является доверенным источником.
        val response = client.post("/posts") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreatePostRequest("spoofed-user-id", "Выполнил задачу!", null))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }
}