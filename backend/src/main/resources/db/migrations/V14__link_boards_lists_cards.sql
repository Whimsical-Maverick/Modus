BEGIN
    IF EXISTS (SELECT 1 FROM board)
        OR EXISTS(SELECT 1 FROM board_list)
        OR EXISTS(select 1 FROM card) THEN
        RAISE EXCEPTION
        'CANT LINK EXISTING board/board_list/card WITHOUT A BACKFILL'
    END IF
END $$