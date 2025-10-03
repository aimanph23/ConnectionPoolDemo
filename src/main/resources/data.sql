-- Insert sample products
INSERT INTO products (name, description, price, stock_quantity, last_updated, created_at) 
VALUES 
    ('Laptop', 'High-performance laptop with 16GB RAM', 1299.99, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Mouse', 'Wireless optical mouse', 29.99, 150, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Keyboard', 'Mechanical keyboard with RGB lighting', 89.99, 75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Monitor', '27-inch 4K monitor', 499.99, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Headphones', 'Noise-cancelling wireless headphones', 199.99, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

