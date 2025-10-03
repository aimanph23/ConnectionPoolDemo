# Postman Echo API Integration Guide

This guide explains how the application now uses the real [Postman Echo delay API](https://postman-echo.com/delay/) for testing connection pool behavior.

## Overview

The application now calls a **real external API** instead of simulating delays with `Thread.sleep()`. This provides more realistic testing conditions for:

- Network latency simulation
- Connection pool behavior under load
- Timeout handling
- Error scenarios

## API Details

### Endpoint

**Postman Echo Delay API**: `https://postman-echo.com/delay/{seconds}`

**Documentation**: https://learning.postman.com/docs/developer/echo-api/

### How It Works

The API delays the response by the specified number of seconds, then returns a JSON response with the delay value.

**Example Request:**
```bash
curl https://postman-echo.com/delay/2
```

**Example Response:**
```json
{
  "delay": "2"
}
```

## Configuration

### application.properties

```properties
# External API Configuration (Postman Echo with configurable delay)
external.delay.api.base-url=https://postman-echo.com/delay
external.delay.api.delay-seconds=2
```

### Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| `external.delay.api.base-url` | `https://postman-echo.com/delay` | Base URL of the delay API |
| `external.delay.api.delay-seconds` | `2` | Delay in seconds (integer) |

## Usage Examples

### Default Configuration (2 seconds)

```bash
# V1 - Blocking (holds DB connection during API call)
curl http://localhost:8080/api/products/1

# V2 - Non-blocking (releases DB connection before API call)
curl http://localhost:8080/api/products/v2/1
```

**Response:**
```json
{
  "id": 1,
  "name": "Laptop",
  "price": 1200.00,
  "message": "External API Response: Product 1 processed successfully (took 2050ms, delay: 2 seconds)"
}
```

### Change Delay Duration

Edit `application.properties`:

```properties
# 1 second delay
external.delay.api.delay-seconds=1

# 5 second delay
external.delay.api.delay-seconds=5

# 10 second delay
external.delay.api.delay-seconds=10
```

### Override at Runtime

```bash
# Run with 3 second delay
mvn spring-boot:run -Dspring-boot.run.arguments=--external.delay.api.delay-seconds=3

# Or with java -jar
java -jar target/connection-pool-demo-1.0.0.jar --external.delay.api.delay-seconds=5
```

## Testing Different Scenarios

### Fast Response (1 second)

```properties
external.delay.api.delay-seconds=1
```

**Use Case:** Test with minimal delay, faster development testing

### Moderate Response (2-3 seconds)

```properties
external.delay.api.delay-seconds=2
```

**Use Case:** Simulate typical API response times

### Slow Response (5-10 seconds)

```properties
external.delay.api.delay-seconds=5
```

**Use Case:** Test connection pool under stress, timeout scenarios

### Very Slow Response (15+ seconds)

```properties
external.delay.api.delay-seconds=15
```

**Use Case:** Test connection pool exhaustion, circuit breaker patterns

## Advantages Over Mock Delay

### Old Approach (Thread.sleep)
```java
// Simulated delay - doesn't test real network behavior
Thread.sleep(2000);
```

❌ No actual network call
❌ No real I/O wait
❌ Doesn't test RestTemplate behavior
❌ Doesn't test timeout handling
❌ Doesn't test network errors

### New Approach (Postman Echo)
```java
// Real HTTP call with actual network delay
restTemplate.getForObject("https://postman-echo.com/delay/2", Map.class);
```

✅ Real network call
✅ Actual I/O wait
✅ Tests RestTemplate behavior
✅ Tests timeout handling
✅ Can test network errors
✅ More realistic performance metrics

## Monitoring

### Watch API Calls in Logs

```bash
# Start application with debug logging
mvn spring-boot:run

# Watch for log entries
tail -f logs/spring.log | grep "Postman Echo"
```

**Example Log Output:**
```
INFO  --- Calling Postman Echo API: https://postman-echo.com/delay/2 for product ID: 1
INFO  --- Expected delay: 2 seconds
INFO  --- External API call completed. Response: External API Response: Product 1 processed successfully (took 2051ms, delay: 2 seconds)
```

### Monitor Connection Pool During API Calls

```bash
# Terminal 1: Send requests
for i in {1..10}; do
  curl http://localhost:8080/api/products/1 &
done

# Terminal 2: Watch connection pool
watch -n 0.5 'curl -s http://localhost:8080/api/monitoring/hikari/status | jq'
```

## Comparison: V1 vs V2 with Real API

### V1 - Blocking Endpoint

```
GET /api/products/1
     ↓
[Acquire DB Connection]
     ↓
[Query Product - 50ms]
     ↓
[HTTP Call to Postman Echo - 2000ms] ← DB Connection HELD
     ↓
[Release DB Connection]
     ↓
[Return Response]

Total Connection Hold: ~2050ms
```

### V2 - Non-Blocking Endpoint

```
GET /api/products/v2/1
     ↓
[Acquire DB Connection]
     ↓
[Query Product - 50ms]
     ↓
[Release DB Connection] ← Connection RELEASED
     ↓
[HTTP Call to Postman Echo - 2000ms] ← No DB Connection
     ↓
[Return Response]

Total Connection Hold: ~50ms
```

## Load Testing

### Test V1 Under Load

```bash
# 20 concurrent requests with 2-second delay each
for i in {1..20}; do
  curl -w "\nTime: %{time_total}s\n" http://localhost:8080/api/products/1 &
done

# Monitor connection pool
curl http://localhost:8080/api/monitoring/hikari/status
```

**Expected Result:**
- Connection pool exhaustion
- Threads waiting for connections
- Slower response times for requests 11-20

### Test V2 Under Load

```bash
# 20 concurrent requests with 2-second delay each
for i in {1..20}; do
  curl -w "\nTime: %{time_total}s\n" http://localhost:8080/api/products/v2/1 &
done

# Monitor connection pool
curl http://localhost:8080/api/monitoring/hikari/status
```

**Expected Result:**
- Healthy connection pool
- No waiting threads
- Consistent response times for all requests

## Error Handling

### Network Timeout

If the API call takes too long:

```properties
# Configure RestTemplate timeout
spring.mvc.async.request-timeout=30000
```

### API Unreachable

The application handles errors gracefully:

```java
try {
    response = restTemplate.getForObject(apiUrl, Map.class);
} catch (Exception e) {
    log.error("Error calling external API: {}", e.getMessage());
    return "External API Error: " + e.getMessage();
}
```

### Rate Limiting

Postman Echo is a free service. For production or heavy load testing, consider:

1. **Self-hosted delay service**
2. **Mock server (WireMock, MockServer)**
3. **Configure shorter delays**

## Production Considerations

### Use in Production

✅ **Safe for development/testing**
❌ **Not recommended for production** (external dependency)

For production, replace with:
- Your actual external APIs
- Self-hosted mock service
- Circuit breaker pattern with fallback

### Alternative APIs

If Postman Echo is unavailable:

```properties
# Use httpbin.org
external.delay.api.base-url=https://httpbin.org/delay

# Use custom delay service
external.delay.api.base-url=http://your-server.com/delay
```

## Troubleshooting

### Issue: Connection refused

**Symptom:** `Connection refused` error in logs

**Solution:** 
- Check internet connectivity
- Verify Postman Echo is accessible: `curl https://postman-echo.com/delay/1`
- Consider using a different delay service

### Issue: Read timeout

**Symptom:** `Read timed out` error

**Solution:**
```properties
# Reduce delay
external.delay.api.delay-seconds=1

# Or increase timeout
spring.mvc.async.request-timeout=15000
```

### Issue: Slow response

**Symptom:** Endpoints taking longer than expected

**Solution:**
- Check configured delay value
- Verify network connectivity
- Monitor connection pool status

## Testing Checklist

Use the real API to verify:

- [x] Connection pool behavior under load
- [x] V1 vs V2 performance difference
- [x] Timeout handling
- [x] Error handling
- [x] Response time metrics
- [x] Connection pool metrics accuracy

## Examples

### Quick Test

```bash
# Test with 1 second delay
echo "external.delay.api.delay-seconds=1" >> application.properties

# Single request
time curl http://localhost:8080/api/products/1
# Should take ~1 second

# V2 comparison
time curl http://localhost:8080/api/products/v2/1
# Should also take ~1 second (but better connection pool usage)
```

### Load Test

```bash
# Set 2 second delay
echo "external.delay.api.delay-seconds=2" >> application.properties

# Run comparison script
./compare-v1-v2.sh
```

### Stress Test

```bash
# Set 5 second delay
echo "external.delay.api.delay-seconds=5" >> application.properties

# Send 30 concurrent requests to V1
for i in {1..30}; do
  curl http://localhost:8080/api/products/1 > /dev/null 2>&1 &
done

# Watch connection pool exhaust
watch -n 0.5 'curl -s http://localhost:8080/api/monitoring/hikari/status'
```

## Summary

| Aspect | Value |
|--------|-------|
| **API** | Postman Echo (https://postman-echo.com/delay/) |
| **Configuration** | `external.delay.api.delay-seconds` |
| **Default Delay** | 2 seconds |
| **Connection Type** | Real HTTP call |
| **Use Case** | Development & testing |
| **Production Ready** | No (external dependency) |

**Key Benefit:** Real network I/O testing for accurate connection pool behavior analysis! 🚀

## Related Documentation

- [MONITORING_GUIDE.md](MONITORING_GUIDE.md) - Monitor connection pool metrics
- [ASYNC_API_GUIDE.md](ASYNC_API_GUIDE.md) - V2 non-blocking implementation
- [README.md](README.md) - Main project documentation

