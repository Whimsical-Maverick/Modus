BEGIN
    IF EXISTS (SELECT 1 FROM board)
        OR EXISTS(SELECT 1 FROM board_list)
        OR EXISTS(select 1 FROM card) THEN
        RAISE EXCEPTION
        'CANT LINK EXISTING board/board_list/card WITHOUT A BACKFILL'
    END IF;
END $$

ALTER TABLE board
    ADD COLUMN workspace_id UUID NOT NULL,
    ADD COLUMN name VARCHAR(120) NOT NULL,
    ADD CONSTRAINT fk_board_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspace(id),
    ADD CONSTRAINT ck_board_name_not_blank
        CHECK (btrim(name) <> '');

CREATE INDEX idx_board_workspace_id ON board(workspace_id);

ALTER TABLE board_list
    ADD COLUMN board_id UUID NOT NULL,
    ADD COLUMN name VARCHAR(120) NOT NULL,
    ADD COLUMN sort_order INTEGER NOT NULL,
    ADD CONSTRAINT fk_board_list_board
        FOREIGN KEY (board_id) REFERENCES board(id),
    ADD CONSTRAINT ck_board_list_name_not_blank
        CHECK (btrim(name) <> ''),
    ADD CONSTRAINT ck_board_list_sort_order
        CHECK (sort_order >= 0),
    ADD CONSTRAINT uq_board_list_sort_order
        UNIQUE (board_id, sort_order);

CREATE INDEX idx_board_list_board_id ON board_list(board_id);

ALTER TABLE card
    ADD COLUMN list_id UUID NOT NULL,
    ADD COLUMN title VARCHAR(200) NOT NULL,
    ADD CONSTRAINT fk_card_list
        FOREIGN KEY (list_id) REFERENCES board_list(id),
    ADD CONSTRAINT ck_card_title_not_blank
        CHECK (btrim(title) <> '');

CREATE INDEX idx_card_list_id ON card(list_id);