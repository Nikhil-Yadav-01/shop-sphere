CREATE TABLE IF NOT EXISTS admin_audit_log
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    admin_id
    BIGINT
    NOT
    NULL,
    action
    VARCHAR
(
    50
) NOT NULL,
    resource_type VARCHAR
(
    50
) NOT NULL,
    resource_id BIGINT NOT NULL,
    change_details TEXT,
    ip_address VARCHAR
(
    45
),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS system_metrics
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    metric_type
    VARCHAR
(
    100
) NOT NULL,
    value NUMERIC
(
    10,
    2
) NOT NULL,
    unit VARCHAR
(
    50
) NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX idx_admin_id ON admin_audit_log (admin_id);
CREATE INDEX idx_action ON admin_audit_log (action);
CREATE INDEX idx_resource ON admin_audit_log (resource_type, resource_id);
CREATE INDEX idx_metric_type ON system_metrics (metric_type);
CREATE INDEX idx_recorded_at ON system_metrics (recorded_at);
