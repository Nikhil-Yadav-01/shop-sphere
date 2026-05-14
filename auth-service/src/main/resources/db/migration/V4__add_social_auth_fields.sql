-- Add social auth fields to users table
ALTER TABLE users ADD COLUMN auth_provider VARCHAR(20) DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255);
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- Index for provider_id lookups
CREATE INDEX idx_users_provider_id ON users (provider_id);
