# Product API Sleep Configuration

## Overview

The `GET /api/products/{id}` endpoint now includes a configurable sleep duration that executes **after** the API call. This is useful for:
- **Load testing**: Simulating slow processing time
- **Performance testing**: Testing thread pool behavior under sustained load
- **Debugging**: Adding artificial delays to observe system behavior

## Configuration

### Property
```properties
# application.properties
product.api.sleep.ms=1000
```

### Parameters
- **Property Name**: `product.api.sleep.ms`
- **Type**: `long` (milliseconds)
- **Default**: `0` (no sleep)
- **Range**: `0` to `Long.MAX_VALUE`

### Examples

**No sleep (default behavior):**
```properties
product.api.sleep.ms=0
```

**1 second sleep:**
```properties
product.api.sleep.ms=1000
```

**5 seconds sleep:**
```properties
product.api.sleep.ms=5000
```

**100 milliseconds sleep:**
```properties
product.api.sleep.ms=100
```

## Endpoint Behavior

### Request Flow

```
1. Client → GET /api/products/{id}
2. Server → Fetch product from database
3. Server → Call Mock API (with its own delay)
4. Server → Sleep for configured duration ⏰ (NEW!)
5. Server → Return response to client
```

### Example Timeline

With `product.api.sleep.ms=2000`:

```
Time    Action
-----   ------
0ms     Request received
10ms    Product fetched from DB
10ms    Mock API called (3 second delay configured)
3010ms  Mock API response received
3010ms  Start sleep (2000ms) ⏰
5010ms  Sleep complete
5010ms  Response sent to client

Total: ~5 seconds
```

## Use Cases

### 1. Load Testing with Sustained Connections

Test how many concurrent requests your system can handle when each request takes time to process:

```properties
# Simulate 2 second processing time per request
product.api.sleep.ms=2000
```

```bash
# Generate 100 concurrent requests
ab -n 1000 -c 100 http://localhost:8080/api/products/1
```

**What to observe:**
- Tomcat thread pool utilization
- HikariCP connection pool usage
- System resource consumption

### 2. Testing Thread Pool Exhaustion

```properties
# Long sleep to hold threads
product.api.sleep.ms=10000
```

```bash
# Flood with requests
ab -n 5000 -c 500 http://localhost:8080/api/products/1
```

**What to observe:**
- Thread pool dashboard shows high active threads
- Requests start queuing or timing out
- Connection pool behavior under pressure

### 3. Comparing Sync vs Async Performance

**Test synchronous endpoint:**
```bash
# With sleep enabled
curl http://localhost:8080/api/products/1
# Takes: DB time + Mock API time + Sleep time
```

**Test asynchronous endpoint:**
```bash
# V2 endpoint doesn't have the sleep
curl http://localhost:8080/api/products/v2/1
# Takes: DB time + Mock API time (no additional sleep)
```

### 4. Simulating Real-World Processing

If your production API does additional processing (e.g., calculations, transformations), simulate it:

```properties
# Simulate 500ms of business logic processing
product.api.sleep.ms=500
```

## Implementation Details

### Code Location
- **File**: `ProductController.java`
- **Method**: `getProductById(@PathVariable Long id)`
- **Line**: After Mock API call

### Code Snippet
```java
// Call mock API with configurable delay
String mockApiResponse = mockApiService.callMockApi(id);
log.info("Mock API response: {}", mockApiResponse);

// Configurable sleep after API call
if (productApiSleepMs > 0) {
    log.info("Sleeping for {} ms after API call", productApiSleepMs);
    Thread.sleep(productApiSleepMs);
}
```

### Error Handling
- **InterruptedException**: Properly handled with thread interrupt restoration
- **Logging**: Sleep duration is logged for debugging
- **Zero/Negative Values**: Sleep is skipped if value is 0 or negative

## Monitoring

### Log Messages

When sleep is active:
```
INFO  - Mock API response: Mock API called with ID: 1
INFO  - Sleeping for 2000 ms after API call
```

When sleep is disabled:
```
INFO  - Mock API response: Mock API called with ID: 1
(no sleep log)
```

### Metrics to Watch

1. **Response Time**
   - Should increase by approximately `product.api.sleep.ms`
   - Check: `curl -w "@curl-format.txt" http://localhost:8080/api/products/1`

2. **Thread Pool**
   - Dashboard: http://localhost:8080/dashboard/threadpool
   - Metrics: http://localhost:8080/api/threadpool/metrics

3. **Connection Pool**
   - Dashboard: http://localhost:8080/dashboard/hikari
   - Metrics: http://localhost:8080/api/monitoring/pool

## Testing

### Quick Test

1. **Set sleep to 3 seconds:**
   ```properties
   product.api.sleep.ms=3000
   ```

2. **Restart application**

3. **Test endpoint:**
   ```bash
   time curl http://localhost:8080/api/products/1
   ```

4. **Expected result:**
   - Response time: ~6 seconds (3s mock API + 3s sleep)
   - Log shows: "Sleeping for 3000 ms after API call"

### Performance Test

```bash
# Terminal 1: Start monitoring
watch -n 1 'curl -s http://localhost:8080/api/threadpool/metrics | jq'

# Terminal 2: Generate load
ab -n 1000 -c 50 -t 60 http://localhost:8080/api/products/1

# Observe thread pool metrics in real-time
```

## Best Practices

1. **Development**: Set to `0` for normal development
2. **Load Testing**: Set to realistic production processing time
3. **Debugging**: Use moderate values (1000-5000ms) for easier observation
4. **Production**: Always set to `0` (or remove the property)

## Troubleshooting

### Sleep not working?

**Check configuration:**
```bash
curl http://localhost:8080/actuator/env | grep product.api.sleep.ms
```

**Check logs:**
```bash
tail -f logs/application.log | grep "Sleeping for"
```

### Application hanging?

**Sleep too long:**
- Reduce `product.api.sleep.ms` value
- Check if combined with mock API delay: Total = Mock API delay + Sleep

**Too many concurrent requests:**
- Reduce concurrent connections in load test
- Increase Tomcat thread pool size

## Related Configuration

This sleep works in combination with:

1. **Mock API Delay** (`mock.api.delay.seconds`)
   - Configured in: `MockApiService`
   - Default: 3 seconds
   - Total delay = Mock API delay + Product API sleep

2. **Tomcat Threads** (`server.tomcat.threads.max`)
   - Default: 400
   - Each sleeping request holds a thread

3. **HikariCP Pool** (`spring.datasource.hikari.maximum-pool-size`)
   - Default: 10
   - DB connection released before sleep (good!)

## Summary

✅ **Added**: Configurable sleep after API call in `GET /api/products/{id}`  
✅ **Property**: `product.api.sleep.ms` (milliseconds)  
✅ **Default**: `0` (disabled)  
✅ **Use Case**: Load testing, performance testing, debugging  
✅ **Location**: After Mock API call, before response  

---

**Example Configuration for Load Testing:**
```properties
# Simulate realistic processing time
product.api.sleep.ms=1500

# With existing mock API delay
mock.api.delay.seconds=2

# Total time per request: ~3.5 seconds
```
