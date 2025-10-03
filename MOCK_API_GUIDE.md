# Mock API Guide

This guide explains the mock API feature with configurable delay added to the GET product endpoint.

## Overview

The `GET /api/products/{id}` endpoint now includes a mock API call that simulates calling an external service with a configurable delay. This is useful for:

- Testing connection pool behavior under load
- Simulating real-world API latency
- Performance testing
- Timeout testing
- Load testing scenarios

## Configuration

The mock API is configured in `application.properties`:

```properties
# Mock API Configuration
mock.api.url=https://mock-api.example.com/product-info
mock.api.delay.milliseconds=2000
```

### Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| `mock.api.url` | `https://mock-api.example.com/product-info` | Fictitious URL (for logging purposes) |
| `mock.api.delay.milliseconds` | `2000` | Delay in milliseconds before response |

## How It Works

```
GET /api/products/1
        │
        ▼
   ┌────────────────────────────────────┐
   │  1. Query Database                 │
   │     Get product by ID              │
   └────────┬───────────────────────────┘
            │
            ▼
   ┌────────────────────────────────────┐
   │  2. Call Mock API                  │
   │     Simulate external API call     │
   │     Wait for configured delay      │
   │     (default: 2000ms)              │
   └────────┬───────────────────────────┘
            │
            ▼
   ┌────────────────────────────────────┐
   │  3. Return Response                │
   │     Product data + mock response   │
   └────────────────────────────────────┘
```

## Usage Examples

### Basic Usage

```bash
# Call endpoint (will take ~2 seconds due to mock delay)
curl http://localhost:8080/api/products/1

# Response includes mock API information
{
  "id": 1,
  "name": "Laptop",
  "description": "High-end laptop",
  "price": 1200.00,
  "stockQuantity": 50,
  "message": "Mock API Response: Product 1 processed successfully (took 2001ms)"
}
```

### Changing the Delay

Edit `application.properties`:

```properties
# Set to 5 seconds
mock.api.delay.milliseconds=5000

# Set to 500ms (fast response)
mock.api.delay.milliseconds=500

# Set to 10 seconds (slow response)
mock.api.delay.milliseconds=10000
```

### Using Environment Variables

Override at runtime:

```bash
# Run with 3 second delay
mvn spring-boot:run -Dspring-boot.run.arguments=--mock.api.delay.milliseconds=3000

# Or with Java
java -jar target/connection-pool-demo-1.0.0.jar --mock.api.delay.milliseconds=3000
```

### Using System Properties

```bash
mvn spring-boot:run -Dmock.api.delay.milliseconds=1500
```

## Testing Scenarios

### 1. Performance Testing

Test how the application handles delays:

```bash
# Test with different delays
for delay in 500 1000 2000 5000; do
  echo "Testing with ${delay}ms delay"
  curl "http://localhost:8080/api/products/1" \
    -H "mock-delay: ${delay}"
done
```

### 2. Load Testing

Simulate multiple concurrent requests:

```bash
# Install Apache Bench
# brew install httpd (macOS)

# 100 requests, 10 concurrent
ab -n 100 -c 10 http://localhost:8080/api/products/1

# View results showing impact of delay
```

### 3. Timeout Testing

Configure application timeout shorter than mock delay to test timeout handling:

```properties
# In application.properties
spring.mvc.async.request-timeout=1000
mock.api.delay.milliseconds=5000
```

### 4. Connection Pool Testing

Monitor connection pool behavior with delays:

```bash
# Watch logs for HikariCP statistics
tail -f logs/spring.log | grep -i hikari
```

## Monitoring

### Log Output

The mock API call produces detailed logs:

```
2024-10-03 10:30:15.123 INFO --- [http-nio-8080-exec-1] c.e.c.service.MockApiService : Calling mock API: https://mock-api.example.com/product-info for product ID: 1
2024-10-03 10:30:15.123 INFO --- [http-nio-8080-exec-1] c.e.c.service.MockApiService : Simulating API delay of 2000 milliseconds
2024-10-03 10:30:17.125 INFO --- [http-nio-8080-exec-1] c.e.c.service.MockApiService : Mock API call completed. Response: Mock API Response: Product 1 processed successfully (took 2001ms)
```

### Measuring Response Time

Using cURL:

```bash
curl -w "\nTotal time: %{time_total}s\n" http://localhost:8080/api/products/1

# Expected: ~2+ seconds (2s mock delay + DB query time)
```

## Advanced Usage

### Custom Delay Per Request

The `MockApiService` includes a method for custom delays:

```java
// In your code
String response = mockApiService.callMockApiWithCustomDelay(productId, 3000L);
```

### Getting Configuration Values

```java
// Get configured delay
long delay = mockApiService.getConfiguredDelay();

// Get mock URL
String url = mockApiService.getMockApiUrl();
```

## Comparison: With vs Without Mock API

| Metric | Without Mock | With Mock (2s delay) |
|--------|-------------|----------------------|
| Response Time | ~50-100ms | ~2050-2100ms |
| DB Queries | 1 | 1 |
| External Calls | 0 | 1 (simulated) |
| Use Case | Fast testing | Realistic simulation |

## Real-World Scenarios

### Simulating Third-Party APIs

```properties
# Payment gateway (typically slow)
mock.api.delay.milliseconds=3000

# Social media API
mock.api.delay.milliseconds=1500

# Internal microservice
mock.api.delay.milliseconds=500
```

### Network Latency Simulation

```properties
# Good network
mock.api.delay.milliseconds=100

# Average network
mock.api.delay.milliseconds=500

# Poor network
mock.api.delay.milliseconds=2000

# Very poor network
mock.api.delay.milliseconds=5000
```

## Disabling the Mock API

To disable the mock delay:

```properties
# Set delay to 0 (instant response)
mock.api.delay.milliseconds=0
```

Or comment out the mock API call in the controller temporarily.

## Best Practices

1. **Development**: Use short delays (100-500ms) for faster testing
2. **Testing**: Use realistic delays (1000-3000ms) to simulate production
3. **Load Testing**: Vary delays to test different scenarios
4. **CI/CD**: Set to 0 or very low values for fast pipeline execution

## Troubleshooting

### Issue: Response takes too long

**Solution**: Reduce the delay:
```properties
mock.api.delay.milliseconds=500
```

### Issue: Connection pool exhausted

**Symptom**: Many concurrent requests with long delays
**Solution**: Increase pool size or reduce delay:
```properties
spring.datasource.hikari.maximum-pool-size=20
mock.api.delay.milliseconds=1000
```

### Issue: Request timeout

**Symptom**: Requests timing out before mock completes
**Solution**: Increase timeout or reduce delay:
```properties
spring.mvc.async.request-timeout=10000
mock.api.delay.milliseconds=3000
```

## Integration with Other Endpoints

Currently, the mock API is only on the GET endpoint. To add to other endpoints:

```java
// In any controller method
String mockResponse = mockApiService.callMockApi(id);
```

## Performance Impact

| Delay Setting | Requests/Second | Avg Response Time |
|--------------|-----------------|-------------------|
| 0ms | ~500-1000 | ~50ms |
| 500ms | ~2 | ~550ms |
| 1000ms | ~1 | ~1050ms |
| 2000ms | ~0.5 | ~2050ms |

*Based on single-threaded requests

## Future Enhancements

Potential improvements:

- Random delay ranges
- Failure simulation (random errors)
- Rate limiting simulation
- Circuit breaker integration
- Conditional delays based on product ID
- Response size simulation

## Related Documentation

- [README.md](README.md) - Main project documentation
- [SETUP_GUIDE.md](SETUP_GUIDE.md) - Setup instructions
- [application.properties](src/main/resources/application.properties) - Configuration file

