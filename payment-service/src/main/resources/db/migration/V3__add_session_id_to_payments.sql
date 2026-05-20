-- Add session_id column to payments table
ALTER TABLE payments ADD COLUMN IF NOT EXISTS session_id VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_payments_session_id ON payments(session_id);
