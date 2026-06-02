CREATE TABLE "user" (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);
CREATE INDEX idx_users_email ON "user"(email);

CREATE TABLE store_credentials (
    store_id VARCHAR(100) PRIMARY KEY,
    public_key TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);
