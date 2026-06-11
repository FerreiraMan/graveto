ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS account_sid UUID DEFAULT NULL;

ALTER TABLE categories
    DROP CONSTRAINT category_name_user_uq;

ALTER TABLE categories
    DROP COLUMN user_sid;

ALTER TABLE categories
    ADD CONSTRAINT category_name_account_uq UNIQUE (name, account_sid);