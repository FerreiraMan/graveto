CREATE SEQUENCE IF NOT EXISTS password_reset_tokens_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS password_reset_tokens
(
    id         BIGINT DEFAULT NEXTVAL('password_reset_tokens_id_seq') PRIMARY KEY,
    sid        UUID UNIQUE         NOT NULL,
    user_id    BIGINT              NOT NULL,
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP           NOT NULL,
    created_at TIMESTAMP           NOT NULL,
    updated_at TIMESTAMP           NOT NULL,

    CONSTRAINT token_user_fk FOREIGN KEY (user_id) REFERENCES users (id)
);

