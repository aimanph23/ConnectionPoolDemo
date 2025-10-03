# Loading Sample Data

This guide explains how to load the 100 sample product records into your YugabyteDB database.

## Available Sample Data Files

1. **`data.sql`** - 5 basic products (auto-loaded by Spring Boot)
2. **`sample-data-100.sql`** - 100 diverse products across 10 categories

## Option 1: Using Spring Boot (Automatic)

By default, Spring Boot will automatically execute `data.sql` on startup, which loads 5 sample products.

To load the 100-record sample instead:

1. Rename or backup the current `data.sql`:
   ```bash
   mv src/main/resources/data.sql src/main/resources/data.sql.backup
   ```

2. Copy the 100-record sample:
   ```bash
   cp src/main/resources/sample-data-100.sql src/main/resources/data.sql
   ```

3. Restart the application:
   ```bash
   mvn spring-boot:run
   ```

## Option 2: Using psql Command Line

Load the sample data manually using the PostgreSQL-compatible `ysqlsh` client:

```bash
# Using ysqlsh (YugabyteDB)
./bin/ysqlsh -h localhost -p 5433 -U yugabyte -d yugabyte -f /path/to/sample-data-100.sql

# Or using standard psql
psql -h localhost -p 5433 -U yugabyte -d yugabyte -f src/main/resources/sample-data-100.sql
```

## Option 3: Using Docker

If YugabyteDB is running in Docker:

```bash
# Copy the SQL file into the container
docker cp src/main/resources/sample-data-100.sql yugabyte-demo:/tmp/

# Execute the SQL file
docker exec -it yugabyte-demo bash -c "cd /home/yugabyte && ./bin/ysqlsh -h localhost -f /tmp/sample-data-100.sql"
```

## Option 4: Using REST API

Create products via the REST API (for selective loading):

```bash
# Example: Create a single product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gaming Laptop Pro",
    "description": "17-inch display, RTX 4080, 32GB RAM",
    "price": 2499.99,
    "stockQuantity": 25
  }'
```

## Option 5: Using DBeaver or pgAdmin

1. Connect to YugabyteDB:
   - Host: `localhost`
   - Port: `5433`
   - Database: `yugabyte`
   - Username: `yugabyte`
   - Password: `yugabyte`

2. Open the `sample-data-100.sql` file

3. Execute the SQL script

## Verify Data Loaded

Check the data was loaded successfully:

```bash
# Via REST API
curl http://localhost:8080/api/products

# Via psql
psql -h localhost -p 5433 -U yugabyte -d yugabyte -c "SELECT COUNT(*) FROM products;"

# Expected output: 100 (or 5 if using default data.sql)
```

## Sample Data Overview

The 100-record dataset includes products across 10 categories:

| Category | Products | Price Range |
|----------|----------|-------------|
| Electronics | 20 | $39.99 - $2,499.99 |
| Home & Kitchen | 15 | $39.99 - $699.99 |
| Sports & Outdoors | 15 | $24.99 - $899.99 |
| Books & Media | 10 | $14.99 - $89.99 |
| Clothing & Accessories | 15 | $19.99 - $349.99 |
| Toys & Games | 10 | $19.99 - $199.99 |
| Office & Stationery | 10 | $24.99 - $449.99 |
| Automotive & Beauty | 5 | $69.99 - $199.99 |

### Stock Quantity Variance
- Low stock: 25-50 units (limited availability items)
- Medium stock: 50-150 units (regular items)
- High stock: 150-600 units (popular/consumable items)

### Price Variance
- Budget items: < $50
- Mid-range: $50 - $200
- Premium: $200 - $700
- Luxury: > $700

## Testing the Process Endpoint

After loading data, test the main endpoint with various products:

```bash
# Process a high-value electronic item
curl -X POST http://localhost:8080/api/products/1/process

# Process a budget item
curl -X POST http://localhost:8080/api/products/51/process

# Process multiple products
for id in {1..10}; do
  curl -X POST http://localhost:8080/api/products/$id/process
  echo ""
done
```

## Clearing Data

To start fresh:

```bash
# Delete all products via psql
psql -h localhost -p 5433 -U yugabyte -d yugabyte -c "TRUNCATE TABLE products RESTART IDENTITY CASCADE;"

# Or via REST API (delete one by one)
curl -X DELETE http://localhost:8080/api/products/1
```

## Troubleshooting

**Issue: Duplicate key error**
- The products table might already have data
- Solution: Clear existing data first or modify the SQL to use `ON CONFLICT DO NOTHING`

**Issue: Permission denied**
- Check YugabyteDB credentials in `application.properties`
- Ensure user has INSERT privileges

**Issue: File not found**
- Verify the file path is correct
- Use absolute path if relative path doesn't work

## Performance Testing

With 100 records, you can perform various tests:

1. **Query Performance**: Test retrieval speeds
2. **Concurrent Updates**: Process multiple products simultaneously
3. **Connection Pool**: Monitor pool usage with load
4. **API Response Times**: Benchmark endpoint performance

Example load test:
```bash
# Install Apache Bench (if not already installed)
# brew install httpd (macOS)

# Test GET endpoint
ab -n 1000 -c 10 http://localhost:8080/api/products/

# Test POST endpoint (requires more complex setup)
```

## Next Steps

- Monitor connection pool statistics in the application logs
- Test the `/process` endpoint with different products
- Observe how stock quantities change based on API responses
- Check YugabyteDB admin UI at http://localhost:7000 for query performance

