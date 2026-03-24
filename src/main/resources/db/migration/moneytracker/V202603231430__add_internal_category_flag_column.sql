ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS is_internal BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE categories
SET sid        = '00000000-0000-0000-0000-000000000000',
    updated_at = now()
WHERE name = 'SYSTEM';

UPDATE categories
SET is_internal = true,
    updated_at  = now()
WHERE sid in ('00000000-0000-0000-0000-000000000000',
              '00000000-0000-0000-0000-000000000001',
              '00000000-0000-0000-0000-000000000002',
              '00000000-0000-0000-0000-000000000003');