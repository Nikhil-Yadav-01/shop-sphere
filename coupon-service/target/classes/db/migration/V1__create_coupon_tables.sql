-- Create coupons table
CREATE TABLE coupons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    description VARCHAR(255) NOT NULL,
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    discount_value DECIMAL(10,2) NOT NULL CHECK (discount_value > 0),
    minimum_order_amount DECIMAL(10,2),
    maximum_discount_amount DECIMAL(10,2),
    usage_limit INTEGER NOT NULL CHECK (usage_limit > 0),
    used_count INTEGER NOT NULL DEFAULT 0 CHECK (used_count >= 0),
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_date_range CHECK (valid_until > valid_from),
    CONSTRAINT usage_within_limit CHECK (used_count <= usage_limit)
);

-- Create coupon_usage table
CREATE TABLE coupon_usage (
    id BIGSERIAL PRIMARY KEY,
    coupon_code VARCHAR(20) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    order_id VARCHAR(255) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL CHECK (discount_amount >= 0),
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_coupons_code ON coupons(code);
CREATE INDEX idx_coupons_active_valid ON coupons(active, valid_from, valid_until);
CREATE INDEX idx_coupon_usage_user_id ON coupon_usage(user_id);
CREATE INDEX idx_coupon_usage_coupon_code ON coupon_usage(coupon_code);
CREATE INDEX idx_coupon_usage_order_id ON coupon_usage(order_id);

-- Insert sample coupons
INSERT INTO coupons (code, description, discount_type, discount_value, minimum_order_amount, maximum_discount_amount, usage_limit, valid_from, valid_until) VALUES
('WELCOME10', '10% off for new customers', 'PERCENTAGE', 10.00, 50.00, 20.00, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days'),
('SAVE20', 'Save $20 on orders over $100', 'FIXED_AMOUNT', 20.00, 100.00, NULL, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '15 days'),
('FLASH50', '50% off flash sale', 'PERCENTAGE', 50.00, 30.00, 100.00, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days');