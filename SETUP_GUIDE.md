# Quick Setup Guide

## Step-by-Step Instructions

### 1. Install YugabyteDB

**Option A: Using Docker (Recommended for quick start)**
```bash
docker run -d --name yugabyte \
  -p 7000:7000 -p 9000:9000 -p 5433:5433 -p 9042:9042 \
  yugabytedb/yugabyte:latest \
  bin/yugabyted start \
  --daemon=false
```

**Option B: Local Installation**
```bash
# Download YugabyteDB
wget https://downloads.yugabyte.com/releases/2.20.0.0/yugabyte-2.20.0.0-b1-darwin-x86_64.tar.gz
tar xvfz yugabyte-2.20.0.0-b1-darwin-x86_64.tar.gz && cd yugabyte-2.20.0.0/

# Start YugabyteDB
./bin/yugabyted start
```

### 2. Verify YugabyteDB is Running

```bash
# Check status
./bin/yugabyted status

# Or with Docker
docker ps | grep yugabyte
```

Access YugabyteDB Admin UI: http://localhost:7000

### 3. Build the Application

```bash
cd /Users/ymmanuel.itable/repo/poc/dbconnpool_demo/ConnectionPoolDemo
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

### 5. Test the Endpoints

**Health Check:**
```bash
curl http://localhost:8080/api/products/health
```

**Create a Product:**
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gaming Mouse",
    "description": "RGB gaming mouse",
    "price": 79.99,
    "stockQuantity": 100
  }'
```

**Process Product (Main Feature):**
```bash
curl -X POST http://localhost:8080/api/products/1/process
```

**Get All Products:**
```bash
curl http://localhost:8080/api/products
```

## What the Main Endpoint Does

The `/api/products/{id}/process` endpoint demonstrates the complete workflow:

1. **Query Database**: Retrieves the product from YugabyteDB
2. **Call External API**: Makes a GET request to JSONPlaceholder API
3. **Update Product**: 
   - Stores the API response in the database
   - Updates stock quantity based on API response:
     - If response title > 50 chars: +10 stock
     - Otherwise: -5 stock (minimum 0)

## Configuration

Default configuration in `src/main/resources/application.properties`:

```properties
# YugabyteDB Connection
spring.datasource.url=jdbc:yugabytedb://localhost:5433/yugabyte?load-balance=true
spring.datasource.username=yugabyte
spring.datasource.password=yugabyte

# Connection Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
```

## Troubleshooting

**Issue: Connection refused**
- Ensure YugabyteDB is running on port 5433
- Check: `telnet localhost 5433`

**Issue: Driver not found**
- Clean and rebuild: `mvn clean install`

**Issue: Port 8080 in use**
- Change port in `application.properties`: `server.port=8081`

## Next Steps

- Explore the API using the included `api-requests.http` file
- Check logs in the console for connection pool statistics
- Monitor YugabyteDB at http://localhost:7000
- Customize the external API URL in `application.properties`

