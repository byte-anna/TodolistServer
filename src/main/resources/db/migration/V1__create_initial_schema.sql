CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(32) NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uq_users_email ON users(email);

CREATE TABLE tasks (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    is_done BOOLEAN NOT NULL DEFAULT FALSE,
    priority INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    due_date TIMESTAMP NULL,
    CONSTRAINT fk_tasks_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_tasks_user_id_created_at ON tasks(user_id, created_at);

CREATE TABLE posts (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    task_id VARCHAR(36) NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_posts_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_posts_task
        FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL
);

CREATE INDEX idx_posts_created_at ON posts(created_at);
CREATE INDEX idx_posts_user_id ON posts(user_id);

CREATE TABLE post_likes (
    id VARCHAR(36) PRIMARY KEY,
    post_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_post_likes_post
        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_likes_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_post_likes_post_id ON post_likes(post_id);
CREATE UNIQUE INDEX uq_post_likes_post_id_user_id ON post_likes(post_id, user_id);
