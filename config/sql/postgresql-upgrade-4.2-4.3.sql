-- MID-6417
ALTER TABLE m_operation_execution ADD COLUMN recordType INT4;

-- MID-3669
ALTER TABLE m_focus ADD COLUMN lockoutStatus INT4;

-- WRITE CHANGES ABOVE ^^
UPDATE m_global_metadata SET value = '4.3' WHERE name = 'databaseSchemaVersion';

COMMIT;
