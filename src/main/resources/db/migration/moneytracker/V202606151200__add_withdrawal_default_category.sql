INSERT INTO categories (sid, name, display_name, parent_id, account_sid, transaction_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'CASH_WITHDRAWAL', 'Cash Withdrawal',
     (SELECT id FROM categories WHERE name = 'FINANCIAL'), null, 'EXPENSE', now(), now());