CREATE TABLE IF NOT EXISTS fraud_detection (
    id SERIAL PRIMARY KEY,
    transaction_id VARCHAR(255) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    risk_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    is_fraudulent BOOLEAN NOT NULL DEFAULT false,
    fraud_reason TEXT,
    status VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50),
    ip_address VARCHAR(45),
    device_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT check_status CHECK (status IN ('PENDING', 'ANALYZING', 'APPROVED', 'REJECTED', 'ESCALATED')),
    CONSTRAINT check_transaction_type CHECK (transaction_type IN ('PURCHASE', 'REFUND', 'CHARGEBACK'))
);

CREATE TABLE IF NOT EXISTS fraud_rules (
    id SERIAL PRIMARY KEY,
    rule_name VARCHAR(255) NOT NULL UNIQUE,
    rule_type VARCHAR(50) NOT NULL,
    threshold NUMERIC(19, 2),
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_fraud_detection_order_id ON fraud_detection(order_id);
CREATE INDEX idx_fraud_detection_customer_id ON fraud_detection(customer_id);
CREATE INDEX idx_fraud_detection_transaction_id ON fraud_detection(transaction_id);
CREATE INDEX idx_fraud_detection_status ON fraud_detection(status);
CREATE INDEX idx_fraud_detection_is_fraudulent ON fraud_detection(is_fraudulent);
CREATE INDEX idx_fraud_rules_enabled ON fraud_rules(enabled);
