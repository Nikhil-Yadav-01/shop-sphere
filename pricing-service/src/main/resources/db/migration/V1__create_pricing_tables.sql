-- Create product_prices table
CREATE TABLE product_prices (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL UNIQUE,
    base_price DECIMAL(10,2) NOT NULL CHECK (base_price > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT product_id_not_empty CHECK (product_id != '')
);

-- Create pricing_rules table
CREATE TABLE pricing_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL CHECK (rule_type IN ('BULK_DISCOUNT', 'SEASONAL', 'TIER_BASED', 'CATEGORY', 'LOCATION')),
    discount_percentage DECIMAL(5,2),
    discount_fixed DECIMAL(10,2),
    min_quantity INTEGER,
    max_quantity INTEGER,
    valid_from TIMESTAMP,
    valid_until TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT discount_check CHECK (discount_percentage IS NOT NULL OR discount_fixed IS NOT NULL),
    CONSTRAINT quantity_check CHECK (min_quantity IS NULL OR max_quantity IS NULL OR min_quantity <= max_quantity)
);

-- Create pricing_tiers table
CREATE TABLE pricing_tiers (
    id BIGSERIAL PRIMARY KEY,
    tier_name VARCHAR(100) NOT NULL,
    min_quantity INTEGER NOT NULL,
    max_quantity INTEGER,
    discount_percentage DECIMAL(5,2) NOT NULL CHECK (discount_percentage >= 0 AND discount_percentage <= 100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT tier_quantity_check CHECK (max_quantity IS NULL OR min_quantity <= max_quantity)
);

-- Create price_history table
CREATE TABLE price_history (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL,
    old_price DECIMAL(10,2) NOT NULL,
    new_price DECIMAL(10,2) NOT NULL,
    changed_by VARCHAR(255),
    change_reason VARCHAR(255),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create price_adjustments table
CREATE TABLE price_adjustments (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL,
    rule_id BIGINT,
    tier_id BIGINT,
    adjustment_percentage DECIMAL(5,2),
    adjustment_fixed DECIMAL(10,2),
    applied_from TIMESTAMP NOT NULL,
    applied_until TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_product_prices_product_id ON product_prices(product_id);
CREATE INDEX idx_product_prices_active ON product_prices(active);
CREATE INDEX idx_pricing_rules_type ON pricing_rules(rule_type);
CREATE INDEX idx_pricing_rules_active ON pricing_rules(is_active);
CREATE INDEX idx_pricing_tiers_quantity ON pricing_tiers(min_quantity, max_quantity);
CREATE INDEX idx_price_history_product_id ON price_history(product_id);
CREATE INDEX idx_price_adjustments_product_id ON price_adjustments(product_id);
CREATE INDEX idx_price_adjustments_active ON price_adjustments(is_active);

-- Insert sample data
INSERT INTO product_prices (product_id, base_price, currency) VALUES
    ('PROD001', 99.99, 'USD'),
    ('PROD002', 199.99, 'USD'),
    ('PROD003', 49.99, 'USD'),
    ('PROD004', 299.99, 'USD'),
    ('PROD005', 149.99, 'USD');

INSERT INTO pricing_tiers (tier_name, min_quantity, max_quantity, discount_percentage) VALUES
    ('Tier 1 - 1-10 units', 1, 10, 0),
    ('Tier 2 - 11-50 units', 11, 50, 5),
    ('Tier 3 - 51-100 units', 51, 100, 10),
    ('Tier 4 - 100+ units', 101, NULL, 15);

INSERT INTO pricing_rules (rule_name, rule_type, discount_percentage, min_quantity, valid_from, valid_until, is_active) VALUES
    ('Summer Sale', 'SEASONAL', 20, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '90 days', true),
    ('Bulk Purchase 50+', 'BULK_DISCOUNT', 15, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '180 days', true),
    ('Bulk Purchase 100+', 'BULK_DISCOUNT', 25, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '180 days', true);
