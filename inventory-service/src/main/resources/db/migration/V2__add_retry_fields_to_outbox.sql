-- Alter outbox_events to add retry count and last error tracking columns
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS last_error TEXT;
