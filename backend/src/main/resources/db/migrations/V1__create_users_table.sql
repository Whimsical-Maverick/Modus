CREATE TABLE users (
        id UUID PRIMARY KEY,
        username VARCHAR(255) NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        email VARCHAR(255) NOT NULL UNIQUE,
        created_at TIMESTAMP NOT NULL DEFAULT now()
);
