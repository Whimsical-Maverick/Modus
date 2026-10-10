ALTER TABLE workspace
    ADD COLUMN name VARCHAR(120) NOT NULL;

ALTER TABLE workspace_member
    ADD COLUMN workspace_id UUID NOT NULL,
    ADD COLUMN user_id UUID NOT NULL,
    ADD COLUMN role VARCHAR(20) NOT NULL;

ALTER TABLE workspace_member
    ADD CONSTRAINT fk_workspace_member_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspace(id)
            ON DELETE CASCADE;

ALTER TABLE workspace_member
    ADD CONSTRAINT fk_workspace_member_user
        FOREIGN KEY (user_id) REFERENCES users(id)
            ON DELETE CASCADE;

ALTER TABLE workspace_member
    ADD CONSTRAINT uq_workspace_member_workspace_user
        UNIQUE (workspace_id, user_id);

CREATE INDEX idx_workspace_member_user_id
    ON workspace_member(user_id);