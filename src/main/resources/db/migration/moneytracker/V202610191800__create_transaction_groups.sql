CREATE SEQUENCE IF NOT EXISTS transaction_groups_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS transaction_groups
(
    id         BIGINT DEFAULT NEXTVAL('transaction_groups_id_seq') PRIMARY KEY,
    sid        UUID UNIQUE                         NOT NULL,
    name       VARCHAR(255) COLLATE ignore_accents NOT NULL,
    created_at TIMESTAMP                           NOT NULL,
    updated_at TIMESTAMP                           NOT NULL,
    created_by UUID                                NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS transaction_groups_creator_lower_name_uq ON transaction_groups (created_by, LOWER(name));

CREATE SEQUENCE IF NOT EXISTS transaction_group_accounts_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS transaction_group_accounts
(
    id         BIGINT DEFAULT NEXTVAL('transaction_group_accounts_id_seq') PRIMARY KEY,
    group_id   BIGINT    NOT NULL,
    account_id BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT transaction_group_accounts_group_fk FOREIGN KEY (group_id) REFERENCES transaction_groups (id),
    CONSTRAINT transaction_group_accounts_account_fk FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT transaction_group_account_uq UNIQUE (group_id, account_id)
);

ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS group_id BIGINT,
    ADD CONSTRAINT transactions_groups_fk FOREIGN KEY (group_id) REFERENCES transaction_groups (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS transactions_group_idx ON transactions (group_id) WHERE group_id IS NOT NULL;
