CREATE SEQUENCE IF NOT EXISTS users_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS users
(
    id         BIGINT DEFAULT nextval('users_id_seq') PRIMARY KEY,
    sid        UUID UNIQUE  NOT NULL,
    email      VARCHAR(255) UNIQUE NOT NULL,
    password   VARCHAR(255),
    role       VARCHAR(50),
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);