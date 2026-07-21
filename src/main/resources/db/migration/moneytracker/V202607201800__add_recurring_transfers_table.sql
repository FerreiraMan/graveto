CREATE SEQUENCE IF NOT EXISTS recurring_transfers_id_seq
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS recurring_transfers
(
    id                     BIGINT  DEFAULT NEXTVAL('recurring_transfers_id_seq') PRIMARY KEY,
    sid                    UUID UNIQUE    NOT NULL,
    source_account_id      BIGINT         NOT NULL,
    destination_account_id BIGINT         NOT NULL,
    user_sid               UUID           NOT NULL,
    description            VARCHAR(255)   NOT NULL,
    amount                 NUMERIC(19, 4) NOT NULL,
    currency               VARCHAR(3)     NOT NULL,
    frequency              VARCHAR(50)    NOT NULL,
    day_of_month           INT,
    day_of_week            INT,
    adjust_to_business_day BOOLEAN DEFAULT TRUE,
    next_execution_date    DATE           NOT NULL,
    last_executed_at       TIMESTAMP,
    status                 VARCHAR(50)    NOT NULL,
    start_date             DATE           NOT NULL,
    end_date               DATE,
    created_at             TIMESTAMP      NOT NULL,
    updated_at             TIMESTAMP      NOT NULL,

    CONSTRAINT recurring_transfer_source_account_fk FOREIGN KEY (source_account_id) REFERENCES accounts (id),
    CONSTRAINT recurring_transfer_destination_account_fk FOREIGN KEY (destination_account_id) REFERENCES accounts (id)
);