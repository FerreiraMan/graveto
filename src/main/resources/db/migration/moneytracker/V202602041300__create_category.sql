CREATE SEQUENCE IF NOT EXISTS categories_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS categories
(
    id         BIGINT DEFAULT nextval('categories_id_seq') PRIMARY KEY,
    sid        UUID UNIQUE  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    parent_id  BIGINT,
    user_sid   UUID,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,

    CONSTRAINT parent_child_fk FOREIGN KEY (parent_id) REFERENCES categories (id),
    CONSTRAINT category_name_user_uq UNIQUE (name, user_sid)
);