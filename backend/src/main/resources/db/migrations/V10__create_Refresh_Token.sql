CREATE TABLE refresh_token (
        id UUID PRIMARY KEY,
        token_hash VARCHAR(64) NOT NULL UNIQUE,
        user_id UUID NOT NULL,
        expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
        revoked_at TIMESTAMP WITH TIME ZONE NULL,

        CONSTRAINT fk_refresh_token_user
            FOREIGN KEY (user_id)
                REFERENCES users(id)
                ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_hash
    ON refresh_token(token_hash);

CREATE INDEX idx_refresh_token_user_id
    ON refresh_token(user_id);