UPDATE categories
SET sid        = '00000000-0000-0000-0000-000000000004',
    updated_at =
        NOW()
WHERE name = 'OTHER'
  AND account_sid IS NULL;

UPDATE categories
SET sid        = '00000000-0000-0000-0000-000000000005',
    updated_at =
        NOW()
WHERE name = 'OTHER_INCOME'
  AND account_sid IS NULL;
