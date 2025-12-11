-- Create inventory_items table
CREATE TABLE IF NOT EXISTS inventory_items (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(255) NOT NULL UNIQUE,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    reorder_level INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    warehouse_location VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create indexes for inventory_items
CREATE INDEX IF NOT EXISTS idx_product_id ON inventory_items(product_id);
CREATE INDEX IF NOT EXISTS idx_sku ON inventory_items(sku);

-- Create stock_movements table
CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGSERIAL PRIMARY KEY,
    inventory_item_id BIGINT NOT NULL,
    movement_type VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    reference VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id) ON DELETE CASCADE
);

-- Create indexes for stock_movements
CREATE INDEX IF NOT EXISTS idx_inventory_id ON stock_movements(inventory_item_id);
CREATE INDEX IF NOT EXISTS idx_movement_type ON stock_movements(movement_type);
CREATE INDEX IF NOT EXISTS idx_created_at ON stock_movements(created_at);
