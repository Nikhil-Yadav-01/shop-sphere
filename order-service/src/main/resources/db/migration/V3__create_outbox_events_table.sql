-- Create outbox_events table for order-service
CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    key VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL
);

-- Create indexes for outbox_events performance
CREATE INDEX IF NOT EXISTS idx_order_outbox_processed ON outbox_events(processed);
CREATE INDEX IF NOT EXISTS idx_order_outbox_created_at ON outbox_events(created_at);
