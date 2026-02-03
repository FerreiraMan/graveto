CREATE SEQUENCE IF NOT EXISTS accounts_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS accounts
(
    id BIGINT DEFAULT nextval('accounts_id_seq') PRIMARY KEY,
    sid UUID UNIQUE NOT NULL,
    balance NUMERIC(19,4) NOT NULL,
    base_currency VARCHAR(3) NOT NULL,
    status VARCHAR(25) NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);