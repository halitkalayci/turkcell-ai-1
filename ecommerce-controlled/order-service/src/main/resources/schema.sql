-- Order Service Schema

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Order line items table
CREATE TABLE IF NOT EXISTS order_line_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- Shipping address table
CREATE TABLE IF NOT EXISTS shipping_addresses (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- Outbox table for transactional outbox pattern
-- Per docs/events/outbox-pattern.md
CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    error_message TEXT
);

-- Indexes for outbox polling efficiency
CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON outbox(status, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON outbox(aggregate_id);
