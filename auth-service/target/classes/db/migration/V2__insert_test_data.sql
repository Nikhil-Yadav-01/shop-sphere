-- Insert test users with pre-hashed passwords
-- Note: These passwords are hashed using BCrypt
-- Password for all test users: TestPassword123!

-- Test Customer User
INSERT INTO users (id, email, password, first_name, last_name, role, enabled, created_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'test.customer@shopsphere.com',
    '$2a$10$X5p9Gt8XGzB1ZX8w0Q5X7eL0L7p7p7p7p7p7p7p7p7p7p7p7p7p7p7p',
    'Test',
    'Customer',
    'CUSTOMER',
    true,
    CURRENT_TIMESTAMP
);

-- Test Seller User
INSERT INTO users (id, email, password, first_name, last_name, role, enabled, created_at)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'test.seller@shopsphere.com',
    '$2a$10$X5p9Gt8XGzB1ZX8w0Q5X7eL0L7p7p7p7p7p7p7p7p7p7p7p7p7p7p7p',
    'Test',
    'Seller',
    'SELLER',
    true,
    CURRENT_TIMESTAMP
);

-- Test Admin User
INSERT INTO users (id, email, password, first_name, last_name, role, enabled, created_at)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    'test.admin@shopsphere.com',
    '$2a$10$X5p9Gt8XGzB1ZX8w0Q5X7eL0L7p7p7p7p7p7p7p7p7p7p7p7p7p7p7p',
    'Test',
    'Admin',
    'ADMIN',
    true,
    CURRENT_TIMESTAMP
);

-- Disabled User (for testing disabled account checks)
INSERT INTO users (id, email, password, first_name, last_name, role, enabled, created_at)
VALUES (
    '44444444-4444-4444-4444-444444444444',
    'disabled@shopsphere.com',
    '$2a$10$X5p9Gt8XGzB1ZX8w0Q5X7eL0L7p7p7p7p7p7p7p7p7p7p7p7p7p7p7p',
    'Disabled',
    'User',
    'CUSTOMER',
    false,
    CURRENT_TIMESTAMP
);
