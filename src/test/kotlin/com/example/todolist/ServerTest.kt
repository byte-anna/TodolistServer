package com.example.todolist

import com.example.todolist.data.db.DatabaseFactory
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskCategory
import com.example.todolist.plugins.AuthResponse
import com.example.todolist.plugins.CreatePostRequest
import com.example.todolist.plugins.CreateTaskRequest
import com.example.todolist.plugins.ErrorResponse
import com.example.todolist.plugins.LoginRequest
import com.example.todolist.plugins.RegisterRequest
import com.example.todolist.plugins.UpdateTaskRequest
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
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
        val task = response.body<Task>()
        assertEquals("Купить молоко", task.title)
        assertEquals(2, task.priority)
        assertEquals(false, task.isDone)
        assertEquals(TaskCategory.NONE, task.category)
    }

    @Test
    fun `POST tasks should preserve priority scale values 1 2 3`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "task_priority_scale_${System.currentTimeMillis()}@mail.com")

        val lowTask = client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Low", priority = 1))
        }.body<Task>()

        val mediumTask = client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Medium", priority = 2))
        }.body<Task>()

        val highTask = client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("High", priority = 3))
        }.body<Task>()

        assertEquals(1, lowTask.priority)
        assertEquals(2, mediumTask.priority)
        assertEquals(3, highTask.priority)
    }

    @Test
    fun `POST tasks with priority above supported scale should return detailed 400`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "task_priority_high_${System.currentTimeMillis()}@mail.com")

        val response = client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Unsupported priority", priority = 4))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = response.body<ErrorResponse>()
        assertEquals(
            "Priority must be in range 1..3 where 1=LOW, 2=MEDIUM, 3=HIGH",
            error.error
        )
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
        val tasks = response.body<List<Task>>()
        assertTrue(tasks.size >= 2)
    }

    @Test
    fun `GET tasks with date query should filter by due date`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "tasks_by_date_${System.currentTimeMillis()}@mail.com")

        client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Task for selected day", dueDate = "2026-06-09T09:00:00"))
        }
        client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Task for another day", dueDate = "2026-06-10T09:00:00"))
        }
        client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Task without due date"))
        }

        val response = client.get("/tasks?date=2026-06-09") {
            bearerAuth(auth.token!!)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val tasks = response.body<List<Task>>()
        assertEquals(1, tasks.size)
        assertEquals("Task for selected day", tasks.single().title)
        assertEquals("2026-06-09T09:00:00", tasks.single().dueDate)
    }

    @Test
    fun `GET tasks with invalid date query should return detailed 400`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "tasks_bad_filter_${System.currentTimeMillis()}@mail.com")

        val response = client.get("/tasks?date=09-06-2026") {
            bearerAuth(auth.token!!)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = response.body<ErrorResponse>()
        assertEquals(
            "Invalid date format. Expected ISO_LOCAL_DATE like 2026-06-09",
            error.error
        )
    }

    @Test
    fun `POST tasks should save selected category`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "task_category_${System.currentTimeMillis()}@mail.com")

        val response = client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Task with category", priority = 3, category = TaskCategory.WORK))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val task = response.body<Task>()
        assertEquals(TaskCategory.WORK, task.category)
    }

    @Test
    fun `POST tasks should return dueDate in ISO local date time format`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "task_due_date_${System.currentTimeMillis()}@mail.com")
        val dueDate = "2026-06-09T14:30:00"

        val response = client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Task with due date", dueDate = dueDate))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val task = response.body<Task>()
        assertEquals(dueDate, task.dueDate)
    }

    @Test
    fun `POST tasks without category should keep backward compatibility`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "task_legacy_${System.currentTimeMillis()}@mail.com")

        val response = client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Legacy task","priority":2}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val task = response.body<Task>()
        assertEquals(TaskCategory.NONE, task.category)
    }

    @Test
    fun `PUT tasks should update category`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "task_update_category_${System.currentTimeMillis()}@mail.com")

        val createdTask = client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Task to update"))
        }.body<Task>()

        val updateResponse = client.put("/tasks/${createdTask.id}") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(UpdateTaskRequest(category = TaskCategory.STUDY))
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)

        val tasks = client.get("/tasks") {
            bearerAuth(auth.token!!)
        }.body<List<Task>>()

        val updatedTask = tasks.first { it.id == createdTask.id }
        assertEquals(TaskCategory.STUDY, updatedTask.category)
    }

    @Test
    fun `PUT tasks should update priority within fixed scale`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "task_update_priority_${System.currentTimeMillis()}@mail.com")

        val createdTask = client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Task to update priority", priority = 1))
        }.body<Task>()

        val updateResponse = client.put("/tasks/${createdTask.id}") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(UpdateTaskRequest(priority = 3))
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)

        val tasks = client.get("/tasks") {
            bearerAuth(auth.token!!)
        }.body<List<Task>>()

        val updatedTask = tasks.first { it.id == createdTask.id }
        assertEquals(3, updatedTask.priority)
    }

    @Test
    fun `PUT tasks with priority above supported scale should return detailed 400`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "task_update_priority_bad_${System.currentTimeMillis()}@mail.com")

        val createdTask = client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Task with invalid update priority"))
        }.body<Task>()

        val response = client.put("/tasks/${createdTask.id}") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(UpdateTaskRequest(priority = 5))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = response.body<ErrorResponse>()
        assertEquals(
            "Priority must be in range 1..3 where 1=LOW, 2=MEDIUM, 3=HIGH",
            error.error
        )
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
        val error = response.body<ErrorResponse>()
        assertEquals(
            "Invalid dueDate format. Expected ISO_LOCAL_DATE_TIME like 2026-06-09T14:30:00",
            error.error
        )
    }

    @Test
    fun `PUT tasks with invalid dueDate should return detailed 400`() = testApplication {
        application { module() }
        val client = jsonClient()

        val auth = registerAndLogin(client, "invalid_due_update_${System.currentTimeMillis()}@mail.com")

        val createdTask = client.post("/tasks") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest("Task to update due date"))
        }.body<Task>()

        val response = client.put("/tasks/${createdTask.id}") {
            bearerAuth(auth.token!!)
            contentType(ContentType.Application.Json)
            setBody(UpdateTaskRequest(dueDate = "09.06.2026 14:30"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = response.body<ErrorResponse>()
        assertEquals(
            "Invalid dueDate format. Expected ISO_LOCAL_DATE_TIME like 2026-06-09T14:30:00",
            error.error
        )
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
        }.body<Task>()

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
