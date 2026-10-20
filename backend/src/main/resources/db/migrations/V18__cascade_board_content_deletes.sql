-- A board or list owns everything below it. Workspace deletion remains
-- restricted: workspaces are archived, not physically deleted.
ALTER TABLE board_list DROP CONSTRAINT fk_board_list_board;
ALTER TABLE board_list ADD CONSTRAINT fk_board_list_board
    FOREIGN KEY (board_id) REFERENCES board(id) ON DELETE CASCADE;

ALTER TABLE card DROP CONSTRAINT fk_card_list;
ALTER TABLE card ADD CONSTRAINT fk_card_list
    FOREIGN KEY (list_id) REFERENCES board_list(id) ON DELETE CASCADE;
