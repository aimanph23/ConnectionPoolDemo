# Version1 Endpoint - Random Product with Timestamp Update

## 🎯 Overview

The `/api/products/version1` endpoint has been enhanced with two key features:
1. **Random Product Selection**: Queries a random product from 1-10,000
2. **Timestamp Update**: Updates the product's `last_updated` field after processing

## 📝 What Changed

### Before:
```java
ProductResponse response = productService.getProductById(1L);  // Always ID 1
// No timestamp update
```

### After:
```java
// Generate random ID between 1 and 10000
long randomId = (long) (Math.random() * 10000) + 1;
ProductResponse response = productService.getProductById(randomId);

// Sleep (configurable)
if (productApiSleepMs > 0) {
    Thread.sleep(productApiSleepMs);
}

// Update timestamp
productService.updateProductTimestamp(randomId);
```

## 🔄 Request Flow

```
1. Client → GET /api/products/version1
2. Server → Generate random ID (1-10000)
3. Server → Query product from database
4. Server → Sleep for configured duration (if enabled)
5. Server → Update product's last_updated timestamp
6. Server → Return product response
```

## 📊 Endpoint Details

### URL
```
GET http://localhost:8080/api/products/version1
```

### Response
```json
{
  "id": 7234,
  "name": "Laptop Pro X1",
  "description": "High-performance laptop with...",
  "price": 1299.99,
  "stockQuantity": 45,
  "lastUpdated": "2025-10-06T12:45:30",
  "externalApiResponse": null,
  "message": null
}
```

### Configuration
```properties
# application.properties
product.api.sleep.ms=2000  # Sleep duration after query
```

## 🎯 Use Cases

### 1. Realistic Load Testing
```bash
# Each request queries a different random product
ab -n 10000 -c 100 http://localhost:8080/api/products/version1

# Benefits:
# - Tests database with varied queries
# - No cache hits (different products each time)
# - Realistic production-like load
# - Tests connection pool with diverse queries
```

### 2. Database Write Testing
```bash
# Each request performs:
# - 1 SELECT (read product)
# - 1 UPDATE (update timestamp)

# With 100 RPS:
# - 100 reads/second
# - 100 writes/second
# - Tests both read and write performance
```

### 3. Timestamp Tracking
```sql
-- Find recently accessed products
SELECT id, name, last_updated 
FROM products 
WHERE last_updated > NOW() - INTERVAL 1 HOUR
ORDER BY last_updated DESC;

-- Find most frequently accessed products
SELECT id, name, COUNT(*) as access_count
FROM products
WHERE last_updated > NOW() - INTERVAL 1 DAY
GROUP BY id, name
ORDER BY access_count DESC
LIMIT 10;
```

## 📈 Performance Impact

### Database Operations Per Request:
1. **SELECT**: Fetch product by random ID
2. **UPDATE**: Update last_updated timestamp
3. **Total**: 2 database operations

### Connection Pool Usage:
```
Request Duration = Query Time + Sleep Time + Update Time

With 2 second sleep:
- Query: ~10ms
- Sleep: 2000ms (holds connection!)
- Update: ~10ms
- Total: ~2020ms

Connection held for: 2020ms
```

### Comparison with Other Endpoints:

| Endpoint | Random? | DB Ops | Writes | Sleep | Connection Time |
|----------|---------|--------|--------|-------|-----------------|
| `/version1` | ✅ Yes | 2 (R+W) | ✅ Yes | ✅ Yes | ~2020ms |
| `/products/{id}` | ❌ No | 1 (R) | ❌ No | ✅ Yes | ~2010ms |
| `/products/v2/{id}` | ❌ No | 1 (R) | ❌ No | ✅ Yes | ~10ms (async!) |

## 🧪 Testing

### Test 1: Basic Functionality
```bash
# Call endpoint multiple times
for i in {1..5}; do
  echo "Request $i:"
  curl -s http://localhost:8080/api/products/version1 | jq '.id, .name, .lastUpdated'
  echo ""
done

# Expected: Different product IDs each time
# Expected: lastUpdated timestamp is current time
```

### Test 2: Verify Timestamp Update
```bash
# Get a product first
PRODUCT_ID=5000
curl http://localhost:8080/api/products/$PRODUCT_ID | jq '.lastUpdated'

# Call version1 endpoint multiple times (might hit that product)
for i in {1..100}; do
  curl -s http://localhost:8080/api/products/version1 > /dev/null
done

# Check if timestamp was updated
curl http://localhost:8080/api/products/$PRODUCT_ID | jq '.lastUpdated'
# Should be more recent if the product was accessed
```

### Test 3: Load Test
```bash
# Generate high load
ab -n 10000 -c 100 http://localhost:8080/api/products/version1

# Monitor:
# - HikariCP Dashboard: Connection pool usage
# - JVM Dashboard: CPU & Memory
# - Database: Write operations
```

### Test 4: Database Impact
```bash
# Monitor database writes
# Terminal 1: Start monitoring
watch -n 1 'curl -s http://localhost:8080/api/jvm/metrics | jq ".memory.heap.usagePercent"'

# Terminal 2: Generate load
ab -n 5000 -c 50 http://localhost:8080/api/products/version1

# Check database size growth (if persistent)
```

## 🔍 Monitoring

### Application Logs
```
INFO  - Received request to get random product, selected ID: 7234
INFO  - Sleeping for 2000 ms after API call
INFO  - Updating last_updated timestamp for product ID: 7234
INFO  - Updating timestamp for product ID: 7234
INFO  - Updated timestamp for product: Laptop Pro X1
```

### Database Queries
```sql
-- Monitor last_updated changes
SELECT 
  id, 
  name, 
  last_updated,
  TIMESTAMPDIFF(SECOND, last_updated, NOW()) as seconds_ago
FROM products
ORDER BY last_updated DESC
LIMIT 20;
```

### Metrics to Watch
1. **Connection Pool**: Active connections during load
2. **CPU Usage**: Should increase with more writes
3. **Memory**: Heap usage during high load
4. **Response Time**: Should be ~2000ms + DB time

## ⚠️ Important Notes

### Connection Pool Consideration
```
With 2 second sleep + timestamp update:
- Each request holds a connection for ~2 seconds
- With 100 concurrent requests: Need 100 connections
- Default pool size: 10 connections
- Result: Connection pool exhaustion!

Solution:
1. Increase pool size
2. Reduce sleep time
3. Use async version (v2)
```

### Database Write Load
```
100 RPS = 100 writes/second
- 6,000 writes/minute
- 360,000 writes/hour
- 8.6 million writes/day

For H2 in-memory: No problem
For production DB: Monitor write performance
```

## 🎯 Recommended Configuration

### For Load Testing (High Write Load):
```properties
# application.properties
product.api.sleep.ms=100  # Shorter sleep

# HikariCP
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=20
```

### For Realistic Testing:
```properties
# application.properties
product.api.sleep.ms=1000  # 1 second

# HikariCP
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10
```

## 📊 Comparison Table

| Feature | version1 | products/{id} | products/v2/{id} |
|---------|----------|---------------|------------------|
| **Random Product** | ✅ Yes | ❌ No | ❌ No |
| **Timestamp Update** | ✅ Yes | ❌ No | ❌ No |
| **Database Writes** | ✅ Yes | ❌ No | ❌ No |
| **Configurable Sleep** | ✅ Yes | ✅ Yes | ✅ Yes |
| **Async Processing** | ❌ No | ❌ No | ✅ Yes |
| **Connection Time** | ~2020ms | ~2010ms | ~10ms |
| **Best For** | Write testing | Read testing | Async testing |

## 🚀 Quick Start

### 1. Configure
```properties
product.api.sleep.ms=2000
spring.sql.init.data-locations=classpath:sample-data-10000.sql
```

### 2. Build & Run
```bash
mvn clean package -DskipTests
java -jar target/connection-pool-demo-1.0.0.jar
```

### 3. Test
```bash
# Single request
curl http://localhost:8080/api/products/version1

# Load test
ab -n 1000 -c 50 http://localhost:8080/api/products/version1
```

### 4. Monitor
```
http://localhost:8080/dashboard/jvm      - CPU & Memory
http://localhost:8080/dashboard/hikari   - Connection Pool
http://localhost:8080/dashboard/tomcat   - HTTP Threads
```

## 📝 Summary

✅ **Random product selection** (1-10,000)  
✅ **Timestamp update** after processing  
✅ **Configurable sleep** duration  
✅ **Comprehensive logging**  
✅ **Perfect for load testing**  
✅ **Tests both reads and writes**  

**Endpoint**: `GET /api/products/version1`  
**Database Operations**: 1 SELECT + 1 UPDATE  
**Use Case**: Realistic load testing with database writes
