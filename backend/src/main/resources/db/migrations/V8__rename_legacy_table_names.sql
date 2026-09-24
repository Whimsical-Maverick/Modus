-- Keep databases created before the table names were standardized compatible
-- with the current @Table mappings.
ALTER TABLE IF EXISTS activitylog RENAME TO activity_log;
ALTER TABLE IF EXISTS boardlist RENAME TO board_list;
ALTER TABLE IF EXISTS workspacemember RENAME TO workspace_member;
