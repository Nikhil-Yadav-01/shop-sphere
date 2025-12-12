CREATE TABLE IF NOT EXISTS media (
    id BIGSERIAL PRIMARY KEY,

    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(150),
    file_size BIGINT NOT NULL,
    file_path VARCHAR(1000) NOT NULL,

    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,

    uploaded_at TIMESTAMP NOT NULL,
    uploaded_by VARCHAR(255),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Optimized indexes for fast lookups
CREATE INDEX IF NOT EXISTS idx_media_entity_type_id 
    ON media(entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_media_entity_type 
    ON media(entity_type);

CREATE INDEX IF NOT EXISTS idx_media_is_active 
    ON media(is_active);

CREATE INDEX IF NOT EXISTS idx_media_uploaded_at 
    ON media(uploaded_at);
