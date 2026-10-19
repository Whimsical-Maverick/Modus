ALTER TABLE activity_log
    ADD COLUMN board_id UUID,
    ADD COLUMN card_id UUID,
    ADD COLUMN actor_user_id UUID,
    ADD COLUMN action VARCHAR(40),
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN created_at TIMESTAMPTZ;

CREATE INDEX idx_activity_log_board_created
    ON activity_log (board_id, created_at DESC);