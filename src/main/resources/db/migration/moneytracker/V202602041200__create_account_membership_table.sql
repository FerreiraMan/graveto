CREATE SEQUENCE IF NOT EXISTS account_membership_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS account_membership
(
    id         BIGINT DEFAULT nextval('account_membership_id_seq') PRIMARY KEY,
    account_id BIGINT       NOT NULL,
    user_sid   UUID,
    role       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,

    CONSTRAINT account_membership_accounts_fk FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT account_membership_account_user_uq UNIQUE (account_id, user_sid)
);
