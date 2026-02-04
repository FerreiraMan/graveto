CREATE SEQUENCE IF NOT EXISTS transactions_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS transactions
(
    id             BIGINT DEFAULT nextval('transactions_id_seq') PRIMARY KEY,
    sid            UUID UNIQUE    NOT NULL,
    amount         NUMERIC(19, 4) NOT NULL,
    currency       VARCHAR(3)     NOT NULL,
    description    VARCHAR(255),
    type           varchar(50)    NOT NULL,
    correlation_id UUID,
    account_id     BIGINT         NOT NULL,
    category_id    BIGINT         NOT NULL,
    status         VARCHAR(50)    NOT NULL,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL,
    deleted_at     TIMESTAMP,
    occurred_at    TIMESTAMP      NOT NULL,

    CONSTRAINT transactions_accounts_fk FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT transactions_categories_fk FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX transactions_account_idx ON transactions (account_id);
CREATE INDEX transactions_correlation_idx ON transactions (correlation_id) WHERE correlation_id IS NOT NULL;