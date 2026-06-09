# TodoListServer

`TodoListServer` — серверная часть приложения TodoList с социальной лентой достижений.

Сервер написан на `Kotlin + Ktor`, хранит данные в `PostgreSQL/Neon`, предоставляет REST API для задач, постов и авторизации, а в тестах использует `H2` в режиме совместимости с PostgreSQL.

## Что реализовано

- сервер на `Ktor`
- внешняя БД `PostgreSQL / Neon`
- JWT-авторизация запросов
- REST API для `auth`, `tasks`, `posts`
- базовое разделение на слои `plugins / domain / data / db`
- автоматические серверные тесты

## Стек технологий

- `Kotlin`
- `Ktor`
- `PostgreSQL / Neon`
- `Exposed`
- `Flyway`
- `HikariCP`
- `JWT`
- `H2` для тестов

## Структура проекта

- `src/main/kotlin/com/example/todolist/plugins` — маршруты, DTO, auth middleware, обработка ошибок
- `src/main/kotlin/com/example/todolist/domain` — модели, интерфейсы репозиториев, use case
- `src/main/kotlin/com/example/todolist/data` — реализация репозиториев и доступ к данным
- `src/main/kotlin/com/example/todolist/data/db` — подключение к БД и описание таблиц
- `src/main/resources` — конфигурация приложения
- `src/test/kotlin` — серверные тесты

## Переменные окружения

Для запуска production/dev сервера нужны:

| Переменная | Назначение |
| --- | --- |
| `DATABASE_URL` | JDBC URL или Postgres URL удалённой БД `PostgreSQL / Neon` |
| `DATABASE_USER` | пользователь БД |
| `DATABASE_PASSWORD` | пароль пользователя БД |
| `JWT_SECRET` | секрет для подписи JWT токенов |

Пример:

```bash
export DATABASE_URL="jdbc:postgresql://<host>:5432/<database>?sslmode=require"
export DATABASE_USER="<database-user>"
export DATABASE_PASSWORD="<database-password>"
export JWT_SECRET="<long-random-secret>"
```

## Локальный запуск

```bash
./gradlew run
```

По умолчанию сервер стартует на `8080`.

## Сборка дистрибутива

```bash
./gradlew installDist
```

После этого готовый дистрибутив лежит в:

```text
build/install/TodoListServer
```

## Docker

Собрать образ:

```bash
docker build -t todolist-server .
```

Запустить контейнер:

```bash
docker run --rm -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://<host>:5432/<database>?sslmode=require" \
  -e DATABASE_USER="<database-user>" \
  -e DATABASE_PASSWORD="<database-password>" \
  -e JWT_SECRET="<long-random-secret>" \
  todolist-server
```

## Тесты

Запуск тестов:

```bash
./gradlew test --no-configuration-cache
```

Тесты не требуют доступа к Neon и работают на локальной `H2` in-memory БД.

## Работа с БД

- в `production/dev` сервер подключается к удалённой `PostgreSQL / Neon` БД
- в тестах используется `H2` в режиме `PostgreSQL`
- `DATABASE_URL` поддерживает как `jdbc:postgresql://...`, так и `postgres://...`
- схема БД создаётся и поддерживается через `Flyway`-миграции из `src/main/resources/db/migration`

## Авторизация

Сейчас сервер использует авторизацию по `email/password` с выдачей JWT токена.

После логина клиент передаёт токен так:

```http
Authorization: Bearer <jwt-token>
```

`userId` для защищённых операций сервер берёт из JWT, а не из тела или query-параметров.

## Основные API endpoints

### Auth

- `POST /auth/register` — регистрация пользователя
- `POST /auth/login` — вход и получение JWT токена

### Tasks

- `GET /tasks` — получить задачи текущего пользователя
- `POST /tasks` — создать задачу
- `PUT /tasks/{id}` — обновить задачу
- `DELETE /tasks/{id}` — удалить задачу

### Posts

- `GET /posts` — получить ленту постов
- `POST /posts` — создать пост
- `POST /posts/{id}/like` — поставить или убрать лайк

## Замечание по клиенту

Требования вроде `Room`, `Navigation`, `Retrofit`, `Hilt`, `Material UI` и `UI tests` относятся к клиентской части и должны быть реализованы в отдельном клиентском репозитории.
