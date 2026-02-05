-- Create products table
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on name for faster searches
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);

-- Insert some sample data for testing
INSERT INTO products (name, description, price) VALUES
    ('Laptop', 'High-performance laptop for developers', 1299.99),
    ('Mouse', 'Wireless ergonomic mouse', 29.99),
    ('Keyboard', 'Mechanical keyboard with RGB lighting', 89.99)
ON CONFLICT DO NOTHING;
