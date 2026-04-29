ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS transaction_type VARCHAR(50);

UPDATE categories
SET transaction_type = 'EXPENSE'
WHERE transaction_type IS NULL;

UPDATE categories
SET transaction_type = 'INCOME'
WHERE name in ('INCOME', 'SALARY', 'FOOD_ALLOWANCE');

UPDATE categories
SET transaction_type = 'TRANSFER_OUT'
WHERE name = 'TRANSFER_OUT';

UPDATE categories
SET transaction_type = 'TRANSFER_IN'
WHERE name = 'TRANSFER_IN';

UPDATE categories
SET transaction_type = 'OPENING_BALANCE'
WHERE name = 'INITIAL_BALANCE';

ALTER TABLE categories
    ALTER COLUMN transaction_type SET NOT NULL;