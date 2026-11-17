CREATE INDEX transaction_group_accounts_account_idx ON transaction_group_accounts (account_id);

ALTER TABLE transaction_group_accounts
    DROP CONSTRAINT transaction_group_accounts_group_fk,
    ADD CONSTRAINT transaction_group_accounts_group_fk
        FOREIGN KEY (group_id) REFERENCES transaction_groups (id) ON DELETE CASCADE;
