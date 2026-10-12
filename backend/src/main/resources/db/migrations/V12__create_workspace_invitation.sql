CREATE TABLE workspace_invitation (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    invited_user_id UUID NOT NULL REFERENCES users(id),
    invited_by_user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    CONSTRAINT ck_workspace_invitation_status
    CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'REVOKED'))
);

CREATE UNIQUE INDEX uq_pending_workspace_invitation
    ON workspace_invitation(workspace_id, invited_user_id)
    WHERE status = 'PENDING';