CREATE TABLE checkout_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL UNIQUE,
    user_id VARCHAR(255) NOT NULL,
    order_number VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    total_amount NUMERIC(19, 2),
    failure_reason VARCHAR(1000),
    idempotency_key VARCHAR(255) UNIQUE,
    version BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cs_user_id ON checkout_sessions(user_id);
CREATE INDEX idx_cs_order_number ON checkout_sessions(order_number);
CREATE INDEX idx_cs_idempotency_key ON checkout_sessions(idempotency_key);
CREATE INDEX idx_cs_session_id ON checkout_sessions(session_id);

CREATE TABLE checkout_outbox_events (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    message_key VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_checkout_outbox_unprocessed ON checkout_outbox_events(processed, created_at);
