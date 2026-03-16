INSERT INTO categories (sid, name, display_name, parent_id, user_sid, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000002', 'TRANSFER_IN', null, (SELECT id FROM categories WHERE name = 'SYSTEM'), null, now(), now()),
    ('00000000-0000-0000-0000-000000000003', 'TRANSFER_OUT', null, (SELECT id FROM categories WHERE name = 'SYSTEM'), null, now(), now());

UPDATE categories
SET sid        = '00000000-0000-0000-0000-000000000001',
    updated_at = now()
WHERE name = 'INITIAL_BALANCE';