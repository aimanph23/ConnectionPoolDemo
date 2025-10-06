#!/usr/bin/env python3
"""
Generate sample-data-10000.sql with 10,000 product records
Based on the format of sample-data-100.sql
"""

import random
from datetime import datetime

# Product categories and templates
CATEGORIES = {
    'Electronics': [
        ('Laptop', 'High-performance laptop with', 799.99, 1999.99),
        ('Smartphone', 'Latest smartphone with', 599.99, 1499.99),
        ('Tablet', 'Portable tablet with', 399.99, 1199.99),
        ('Headphones', 'Premium headphones with', 99.99, 499.99),
        ('Smart Watch', 'Feature-rich smartwatch with', 199.99, 799.99),
        ('Monitor', 'High-resolution monitor with', 299.99, 1299.99),
        ('Keyboard', 'Mechanical keyboard with', 79.99, 299.99),
        ('Mouse', 'Ergonomic mouse with', 29.99, 149.99),
        ('Webcam', 'HD webcam with', 59.99, 249.99),
        ('Speaker', 'Bluetooth speaker with', 49.99, 399.99),
    ],
    'Home & Kitchen': [
        ('Coffee Maker', 'Programmable coffee maker with', 49.99, 299.99),
        ('Blender', 'High-speed blender with', 59.99, 399.99),
        ('Air Fryer', 'Digital air fryer with', 79.99, 249.99),
        ('Vacuum Cleaner', 'Robot vacuum with', 199.99, 799.99),
        ('Microwave', 'Smart microwave with', 99.99, 399.99),
        ('Toaster', 'Stainless steel toaster with', 29.99, 149.99),
        ('Mixer', 'Stand mixer with', 149.99, 599.99),
        ('Pressure Cooker', 'Multi-function cooker with', 79.99, 299.99),
        ('Food Processor', 'Professional processor with', 99.99, 499.99),
        ('Dishwasher', 'Energy-efficient dishwasher with', 399.99, 999.99),
    ],
    'Sports & Outdoors': [
        ('Yoga Mat', 'Non-slip yoga mat with', 19.99, 79.99),
        ('Dumbbell Set', 'Adjustable dumbbells with', 99.99, 499.99),
        ('Treadmill', 'Folding treadmill with', 399.99, 1999.99),
        ('Bicycle', 'Mountain bike with', 299.99, 1499.99),
        ('Tent', 'Camping tent with', 79.99, 399.99),
        ('Sleeping Bag', 'Insulated sleeping bag with', 39.99, 199.99),
        ('Backpack', 'Hiking backpack with', 49.99, 249.99),
        ('Water Bottle', 'Insulated bottle with', 19.99, 59.99),
        ('Fitness Tracker', 'Activity tracker with', 49.99, 199.99),
        ('Running Shoes', 'Performance shoes with', 79.99, 199.99),
    ],
    'Books': [
        ('Fiction Novel', 'Bestselling novel with', 9.99, 29.99),
        ('Cookbook', 'Recipe collection with', 19.99, 49.99),
        ('Self-Help Book', 'Motivational guide with', 14.99, 34.99),
        ('Technical Manual', 'Programming guide with', 39.99, 79.99),
        ('Biography', 'Life story with', 19.99, 39.99),
        ('Children Book', 'Illustrated story with', 9.99, 24.99),
        ('Textbook', 'Educational textbook with', 49.99, 199.99),
        ('Magazine', 'Monthly magazine with', 4.99, 14.99),
        ('Comic Book', 'Graphic novel with', 12.99, 29.99),
        ('Dictionary', 'Comprehensive dictionary with', 29.99, 79.99),
    ],
    'Clothing': [
        ('T-Shirt', 'Cotton t-shirt with', 14.99, 39.99),
        ('Jeans', 'Denim jeans with', 39.99, 99.99),
        ('Jacket', 'Waterproof jacket with', 79.99, 299.99),
        ('Sneakers', 'Casual sneakers with', 49.99, 149.99),
        ('Dress', 'Elegant dress with', 59.99, 199.99),
        ('Sweater', 'Wool sweater with', 39.99, 129.99),
        ('Shorts', 'Athletic shorts with', 24.99, 59.99),
        ('Hat', 'Baseball cap with', 14.99, 39.99),
        ('Socks', 'Performance socks with', 9.99, 24.99),
        ('Belt', 'Leather belt with', 19.99, 59.99),
    ],
    'Toys & Games': [
        ('Board Game', 'Strategy game with', 19.99, 79.99),
        ('Action Figure', 'Collectible figure with', 14.99, 49.99),
        ('Puzzle', '1000-piece puzzle with', 12.99, 39.99),
        ('LEGO Set', 'Building set with', 29.99, 299.99),
        ('Doll', 'Fashion doll with', 19.99, 79.99),
        ('RC Car', 'Remote control car with', 39.99, 199.99),
        ('Video Game', 'Popular game with', 39.99, 69.99),
        ('Plush Toy', 'Soft toy with', 14.99, 49.99),
        ('Card Game', 'Trading cards with', 9.99, 99.99),
        ('Educational Toy', 'Learning toy with', 24.99, 79.99),
    ],
    'Office Supplies': [
        ('Office Chair', 'Ergonomic chair with', 149.99, 599.99),
        ('Desk', 'Standing desk with', 299.99, 999.99),
        ('Printer', 'Wireless printer with', 99.99, 399.99),
        ('Notebook', 'Hardcover notebook with', 9.99, 29.99),
        ('Pen Set', 'Premium pens with', 19.99, 79.99),
        ('File Cabinet', 'Metal cabinet with', 79.99, 299.99),
        ('Desk Lamp', 'LED lamp with', 29.99, 99.99),
        ('Calculator', 'Scientific calculator with', 14.99, 49.99),
        ('Stapler', 'Heavy-duty stapler with', 9.99, 39.99),
        ('Whiteboard', 'Magnetic board with', 39.99, 149.99),
    ],
    'Automotive': [
        ('Car Vacuum', 'Portable vacuum with', 39.99, 129.99),
        ('Dash Cam', 'HD dash camera with', 79.99, 299.99),
        ('Phone Mount', 'Magnetic mount with', 14.99, 39.99),
        ('Car Charger', 'Fast charger with', 19.99, 49.99),
        ('Floor Mats', 'All-weather mats with', 39.99, 99.99),
        ('Seat Covers', 'Universal covers with', 49.99, 149.99),
        ('Jump Starter', 'Portable starter with', 59.99, 199.99),
        ('Tire Inflator', 'Digital inflator with', 29.99, 89.99),
        ('Car Wax', 'Premium wax with', 19.99, 49.99),
        ('Tool Kit', 'Emergency kit with', 39.99, 129.99),
    ],
    'Beauty & Personal Care': [
        ('Hair Dryer', 'Professional dryer with', 49.99, 199.99),
        ('Electric Shaver', 'Cordless shaver with', 79.99, 299.99),
        ('Makeup Set', 'Complete set with', 39.99, 149.99),
        ('Perfume', 'Designer fragrance with', 49.99, 199.99),
        ('Skincare Set', 'Anti-aging set with', 59.99, 249.99),
        ('Hair Straightener', 'Ceramic straightener with', 39.99, 149.99),
        ('Electric Toothbrush', 'Sonic toothbrush with', 49.99, 199.99),
        ('Nail Kit', 'Manicure set with', 29.99, 79.99),
        ('Massage Gun', 'Percussion massager with', 79.99, 299.99),
        ('Scale', 'Smart scale with', 29.99, 99.99),
    ],
    'Food & Beverages': [
        ('Coffee Beans', 'Premium beans with', 12.99, 39.99),
        ('Tea Set', 'Assorted teas with', 19.99, 59.99),
        ('Protein Powder', 'Whey protein with', 29.99, 79.99),
        ('Snack Box', 'Variety pack with', 19.99, 49.99),
        ('Olive Oil', 'Extra virgin oil with', 14.99, 39.99),
        ('Spice Set', 'Gourmet spices with', 24.99, 59.99),
        ('Energy Drinks', '24-pack with', 19.99, 39.99),
        ('Chocolate Box', 'Assorted chocolates with', 14.99, 49.99),
        ('Nuts Mix', 'Premium nuts with', 12.99, 34.99),
        ('Honey', 'Organic honey with', 14.99, 39.99),
    ],
}

# Feature descriptors
FEATURES = [
    'advanced features', 'premium quality', 'latest technology', 'innovative design',
    'superior performance', 'enhanced durability', 'smart functionality', 'eco-friendly materials',
    'wireless connectivity', 'fast charging', 'long battery life', 'compact design',
    'professional grade', 'user-friendly interface', 'multi-function capability', 'energy efficient',
    'water resistant', 'lightweight construction', 'adjustable settings', 'automatic operation',
    'high capacity', 'quick setup', 'universal compatibility', 'noise reduction',
    'temperature control', 'digital display', 'remote control', 'app integration',
    'voice control', 'LED indicators', 'safety features', 'warranty included',
]

def generate_product(product_id):
    """Generate a single product INSERT statement"""
    # Select random category and product type
    category = random.choice(list(CATEGORIES.keys()))
    product_type, desc_prefix, min_price, max_price = random.choice(CATEGORIES[category])
    
    # Generate product name with variation
    variation = random.choice(['Pro', 'Plus', 'Elite', 'Premium', 'Deluxe', 'Ultra', 
                              'Max', 'Advanced', 'Professional', 'Standard', 
                              'Basic', 'Essential', 'Classic', 'Modern', 'Smart'])
    model = random.choice(['X1', 'X2', 'X3', 'Series A', 'Series B', 'Gen 2', 'Gen 3', 
                          'V2', 'V3', 'Model S', 'Model M', 'Model L', '2024', '2025'])
    
    name = f"{product_type} {variation} {model}"
    
    # Generate description
    features = random.sample(FEATURES, 3)
    description = f"{desc_prefix} {', '.join(features)}"
    
    # Generate price and stock
    price = round(random.uniform(min_price, max_price), 2)
    stock = random.randint(10, 500)
    
    return f"('{name}', '{description}', {price}, {stock}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"

def generate_sql_file(num_products=10000, output_file='sample-data-10000.sql'):
    """Generate SQL file with specified number of products"""
    print(f"Generating {num_products} products...")
    
    with open(output_file, 'w') as f:
        # Write header
        f.write(f"-- Sample data with {num_products} products\n")
        f.write("-- Auto-generated for load testing and performance benchmarking\n")
        f.write(f"-- Generated on: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write("-- Categories: Electronics, Home & Kitchen, Sports, Books, Clothing, Toys, Office, Automotive, Beauty, Food\n\n")
        
        # Generate products in batches of 100 for better readability
        batch_size = 100
        for batch_start in range(0, num_products, batch_size):
            batch_end = min(batch_start + batch_size, num_products)
            
            f.write(f"-- Products {batch_start + 1}-{batch_end}\n")
            f.write("INSERT INTO products (name, description, price, stock_quantity, last_updated, created_at) VALUES\n")
            
            products = []
            for i in range(batch_start, batch_end):
                products.append(generate_product(i + 1))
            
            # Write all products in this batch
            f.write(',\n'.join(products))
            f.write(';\n\n')
            
            # Progress indicator
            if (batch_start + batch_size) % 1000 == 0:
                print(f"  Generated {batch_start + batch_size} products...")
    
    print(f"✅ Successfully generated {num_products} products in {output_file}")
    print(f"   File size: {get_file_size(output_file)}")

def get_file_size(filename):
    """Get human-readable file size"""
    import os
    size = os.path.getsize(filename)
    for unit in ['B', 'KB', 'MB', 'GB']:
        if size < 1024.0:
            return f"{size:.2f} {unit}"
        size /= 1024.0
    return f"{size:.2f} TB"

if __name__ == '__main__':
    import sys
    
    # Get number of products from command line or use default
    num_products = 10000
    if len(sys.argv) > 1:
        try:
            num_products = int(sys.argv[1])
        except ValueError:
            print(f"Invalid number: {sys.argv[1]}, using default: 10000")
    
    # Generate the file
    output_file = 'src/main/resources/sample-data-10000.sql'
    generate_sql_file(num_products, output_file)
    
    print(f"\n📝 To use this file:")
    print(f"   1. Update application.properties:")
    print(f"      spring.sql.init.data-locations=classpath:sample-data-10000.sql")
    print(f"   2. Restart the application")
    print(f"   3. Database will be populated with {num_products} products")
