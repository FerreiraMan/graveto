CREATE SEQUENCE IF NOT EXISTS asset_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS assets
(
    id            BIGINT DEFAULT nextval('asset_id_seq') PRIMARY KEY,
    sid           UUID UNIQUE        NOT NULL,
    ticker        VARCHAR(25) UNIQUE NOT NULL,
    name          VARCHAR(255)       NOT NULL,
    asset_type    VARCHAR(50)        NOT NULL,
    currency      VARCHAR(3)         NOT NULL,
    isin          VARCHAR(255),
    current_price NUMERIC(19, 8),
    created_at    TIMESTAMP          NOT NULL,
    updated_at    TIMESTAMP          NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS broker_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS brokers
(
    id          BIGINT DEFAULT nextval('broker_id_seq') PRIMARY KEY,
    sid         UUID UNIQUE  NOT NULL,
    account_sid UUID,
    name        VARCHAR(255) NOT NULL,
    currency    VARCHAR(3)   NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS broker_membership_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS broker_membership
(
    id         BIGINT DEFAULT nextval('broker_membership_id_seq') PRIMARY KEY,
    broker_id  BIGINT      NOT NULL,
    user_sid   UUID        NOT NULL,
    role       VARCHAR(50) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,

    CONSTRAINT broker_membership_brokers_fk FOREIGN KEY (broker_id) REFERENCES brokers (id),
    CONSTRAINT broker_membership_broker_user_uq UNIQUE (broker_id, user_sid)
);

CREATE SEQUENCE IF NOT EXISTS orders_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS orders
(
    id             BIGINT                  DEFAULT nextval('orders_id_seq') PRIMARY KEY,
    sid            UUID UNIQUE    NOT NULL,
    broker_id      BIGINT         NOT NULL,
    asset_id       BIGINT         NOT NULL,
    user_sid       UUID           NOT NULL,
    order_type     VARCHAR(50)    NOT NULL,
    quantity       NUMERIC(19, 8) NOT NULL,
    price_per_unit NUMERIC(19, 8) NOT NULL,
    fees           NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency       VARCHAR(3)     NOT NULL,
    executed_at    TIMESTAMP      NOT NULL,
    notes          VARCHAR(255),
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL,

    CONSTRAINT order_broker_fk FOREIGN KEY (broker_id) REFERENCES brokers (id),
    CONSTRAINT order_asset_fk FOREIGN KEY (asset_id) REFERENCES assets (id)
);

CREATE SEQUENCE IF NOT EXISTS positions_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS positions
(
    id             BIGINT DEFAULT nextval('positions_id_seq') PRIMARY KEY,
    sid            UUID UNIQUE    NOT NULL,
    broker_id      BIGINT         NOT NULL,
    asset_id       BIGINT         NOT NULL,
    quantity       NUMERIC(19, 8) NOT NULL,
    average_cost   NUMERIC(19, 8) NOT NULL,
    total_invested NUMERIC(19, 4) NOT NULL,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL,

    CONSTRAINT position_broker_fk FOREIGN KEY (broker_id) REFERENCES brokers (id),
    CONSTRAINT position_asset_fk FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT position_broker_asset_uq UNIQUE (broker_id, asset_id)
);

CREATE INDEX IF NOT EXISTS orders_broker_idx ON orders (broker_id);
CREATE INDEX IF NOT EXISTS orders_asset_idx ON orders (asset_id);