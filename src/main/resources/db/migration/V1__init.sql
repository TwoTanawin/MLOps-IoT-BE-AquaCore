CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Table: users
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    google_id VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS ponds (
    id BIGSERIAL PRIMARY KEY,
    address VARCHAR(255) NOT NULL,
    serial_number VARCHAR(100) UNIQUE,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_pond_user FOREIGN KEY (user_id) REFERENCES users(id)
);
