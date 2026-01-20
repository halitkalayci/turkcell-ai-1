-- Inventory Service Schema

-- Inventory table
CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL UNIQUE,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Inbox table for idempotency pattern
-- Per docs/events/idempotency.md
CREATE TABLE IF NOT EXISTS inbox (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL
);
