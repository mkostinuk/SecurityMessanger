CREATE TABLE IF NOT EXISTS users (
    id            SERIAL PRIMARY KEY,
    username      VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_banned     BOOLEAN NOT NULL DEFAULT FALSE,
    display_name  VARCHAR(50),
    phone         VARCHAR(30),
    about         TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(30);
ALTER TABLE users ADD COLUMN IF NOT EXISTS about TEXT;

CREATE TABLE IF NOT EXISTS messages (
    id         SERIAL PRIMARY KEY,
    sender_id  INTEGER NOT NULL REFERENCES users(id),
    content    TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
