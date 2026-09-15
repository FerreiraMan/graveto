UPDATE categories
SET name         = 'INVESTMENTS_EXPENSE',
    display_name = 'Investments (Expenses)',
    updated_at   = NOW()
WHERE name = 'INVESTMENTS'
  AND account_sid IS NULL
  AND transaction_type = 'EXPENSE';

INSERT INTO categories (sid, name, display_name, parent_id, account_sid, created_at, updated_at, transaction_type)
VALUES (gen_random_uuid(), 'INVESTMENTS_INCOME', 'Investments (Income)', NULL, NULL, NOW(), NOW(), 'INCOME');

UPDATE categories
SET parent_id  = (SELECT id FROM categories WHERE name = 'INVESTMENTS_INCOME' AND account_sid IS NULL),
    updated_at = NOW()
WHERE name IN ('DIVIDENDS_AND_INTEREST', 'ASSET_SALE')
  AND account_sid IS NULL;
