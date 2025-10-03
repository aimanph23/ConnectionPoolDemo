# HikariCP Monitoring Guide

This guide explains how to monitor HikariCP connection pool metrics in real-time.

## Overview

The application includes monitoring endpoints that expose HikariCP connection pool metrics. This is essential for:

- Performance tuning
- Debugging connection issues
- Load testing analysis
- Production monitoring
- Identifying connection leaks

## Monitoring Endpoints

### 1. Basic Metrics - `/api/monitoring/hikari`

Returns structured connection pool information.

**Request:**
```bash
curl http://localhost:8080/api/monitoring/hikari
```

**Response:**
```json
{
  "poolName": "YugabyteHikariPool",
  "totalConnections": 8,
  "activeConnections": 3,
  "idleConnections": 5,
  "threadsAwaitingConnection": 0,
  "maximumPoolSize": 10,
  "minimumIdle": 5,
  "connectionTimeout": 30000,
  "idleTimeout": 600000,
  "maxLifetime": 1800000,
  "status": "healthy",
  "timestamp": 1696348800000
}
```

### 2. Detailed Metrics - `/api/monitoring/hikari/details`

Returns comprehensive metrics with health indicators.

**Request:**
```bash
curl http://localhost:8080/api/monitoring/hikari/details
```

**Response:**
```json
{
  "poolName": "YugabyteHikariPool",
  "status": "healthy",
  "currentState": {
    "totalConnections": 8,
    "activeConnections": 3,
    "idleConnections": 5,
    "threadsAwaitingConnection": 0
  },
  "configuration": {
    "maximumPoolSize": 10,
    "minimumIdle": 5,
    "connectionTimeout": "30000ms",
    "idleTimeout": "600000ms",
    "maxLifetime": "1800000ms",
    "jdbcUrl": "jdbc:yugabytedb://localhost:5433/yugabyte",
    "driverClassName": "com.yugabyte.Driver"
  },
  "health": {
    "poolUtilization": "30.00%",
    "hasWaitingThreads": false,
    "isPoolFull": false,
    "hasIdleConnections": true
  },
  "timestamp": 1696348800000
}
```

### 3. Quick Status - `/api/monitoring/hikari/status`

Returns minimal metrics for quick checks.

**Request:**
```bash
curl http://localhost:8080/api/monitoring/hikari/status
```

**Response:**
```json
{
  "active": 3,
  "idle": 5,
  "total": 8,
  "waiting": 0,
  "max": 10,
  "healthy": true
}
```

## Metrics Explained

| Metric | Description | Healthy Range |
|--------|-------------|---------------|
| **totalConnections** | Current number of connections in pool | ≤ maximumPoolSize |
| **activeConnections** | Connections currently in use | < maximumPoolSize |
| **idleConnections** | Connections available for use | ≥ minimumIdle |
| **threadsAwaitingConnection** | Threads waiting for a connection | 0 (ideally) |
| **maximumPoolSize** | Maximum connections allowed | Based on load |
| **minimumIdle** | Minimum idle connections maintained | Usually 25-50% of max |
| **connectionTimeout** | Max wait time for connection (ms) | 30000 (30s) |
| **poolUtilization** | Percentage of pool in use | < 80% |

## Health Indicators

### Healthy Pool
```json
{
  "active": 3,
  "idle": 7,
  "total": 10,
  "waiting": 0,
  "poolUtilization": "30.00%"
}
```
✅ Good: Plenty of idle connections, no waiting threads

### Stressed Pool
```json
{
  "active": 9,
  "idle": 1,
  "total": 10,
  "waiting": 5,
  "poolUtilization": "90.00%"
}
```
⚠️ Warning: High utilization, threads are waiting

### Exhausted Pool
```json
{
  "active": 10,
  "idle": 0,
  "total": 10,
  "waiting": 25,
  "poolUtilization": "100.00%"
}
```
🚨 Critical: Pool is full, many threads waiting

## Monitoring During Load Testing

### Watch Metrics in Real-Time

```bash
# Poll every 2 seconds
watch -n 2 'curl -s http://localhost:8080/api/monitoring/hikari/status'

# Or using a loop
while true; do
  clear
  curl -s http://localhost:8080/api/monitoring/hikari/status | jq
  sleep 2
done
```

### Monitor During JMeter Test

```bash
# Terminal 1: Run JMeter test
jmeter -n -t performance-test.jmx -l results.jtl

# Terminal 2: Watch connection pool
watch -n 1 'curl -s http://localhost:8080/api/monitoring/hikari/status | jq'
```

## Integration with Monitoring Tools

### Prometheus Integration

Add to your Prometheus configuration:

```yaml
scrape_configs:
  - job_name: 'spring-boot-hikari'
    metrics_path: '/api/monitoring/hikari/status'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:8080']
```

### Grafana Dashboard Query

```
Example query for active connections:
http_request{endpoint="/api/monitoring/hikari/status", metric="active"}
```

### Custom Monitoring Script

```bash
#!/bin/bash
# monitor-hikari.sh

ENDPOINT="http://localhost:8080/api/monitoring/hikari/status"
ALERT_THRESHOLD=8

while true; do
  ACTIVE=$(curl -s $ENDPOINT | jq -r '.active')
  WAITING=$(curl -s $ENDPOINT | jq -r '.waiting')
  
  echo "$(date) - Active: $ACTIVE, Waiting: $WAITING"
  
  if [ "$ACTIVE" -ge "$ALERT_THRESHOLD" ]; then
    echo "⚠️  WARNING: High connection usage!"
  fi
  
  if [ "$WAITING" -gt "0" ]; then
    echo "🚨 ALERT: Threads waiting for connections!"
  fi
  
  sleep 5
done
```

## Troubleshooting Scenarios

### Scenario 1: Connection Pool Exhaustion

**Symptoms:**
```json
{
  "active": 10,
  "idle": 0,
  "waiting": 15
}
```

**Solutions:**
1. Increase pool size:
   ```properties
   spring.datasource.hikari.maximum-pool-size=20
   ```

2. Optimize query performance (reduce connection hold time)

3. Check for connection leaks

### Scenario 2: Too Many Idle Connections

**Symptoms:**
```json
{
  "active": 2,
  "idle": 18,
  "total": 20
}
```

**Solutions:**
1. Reduce pool size:
   ```properties
   spring.datasource.hikari.maximum-pool-size=10
   ```

2. Adjust minimum idle:
   ```properties
   spring.datasource.hikari.minimum-idle=5
   ```

### Scenario 3: Connections Not Released

**Symptoms:**
- `activeConnections` stays high
- `idleConnections` stays low
- Application slow but no waiting threads

**Solutions:**
1. Check for missing `@Transactional` boundaries
2. Look for long-running queries
3. Review connection timeout settings
4. Check for application deadlocks

## Performance Testing Checklist

Use monitoring endpoints to verify:

- [ ] Pool doesn't exhaust during peak load
- [ ] No threads waiting for connections
- [ ] Pool utilization stays below 80%
- [ ] Idle connections available
- [ ] Connections properly released after use
- [ ] Total connections match expectations

## Best Practices

### Development
```bash
# Quick check during development
curl http://localhost:8080/api/monitoring/hikari/status
```

### Testing
```bash
# Monitor during load tests
watch -n 1 'curl -s http://localhost:8080/api/monitoring/hikari/details | jq .health'
```

### Production
```bash
# Set up automated monitoring
# Alert when waiting > 0 or utilization > 80%
```

## Example Use Cases

### 1. Verify Pool Configuration

```bash
# Check current settings
curl http://localhost:8080/api/monitoring/hikari/details | jq .configuration

{
  "maximumPoolSize": 10,
  "minimumIdle": 5,
  "connectionTimeout": "30000ms",
  ...
}
```

### 2. Monitor During Startup

```bash
# Watch pool initialization
for i in {1..10}; do
  curl -s http://localhost:8080/api/monitoring/hikari/status
  sleep 1
done
```

### 3. Debug Slow Responses

```bash
# Check if connections are the bottleneck
curl http://localhost:8080/api/monitoring/hikari/status

# If waiting > 0, pool is likely the issue
# If waiting = 0, issue is elsewhere (DB, network, etc.)
```

### 4. Capacity Planning

Monitor under load to determine optimal pool size:

```bash
# Run load test while monitoring
# Gradually increase load until you see waiting threads
# That's your connection pool capacity limit
```

## API Response Times

| Endpoint | Typical Response Time |
|----------|----------------------|
| `/hikari` | < 10ms |
| `/hikari/details` | < 15ms |
| `/hikari/status` | < 5ms |

These are lightweight endpoints designed for frequent polling.

## Security Considerations

In production, consider:

1. **Authentication**: Protect monitoring endpoints
2. **Rate Limiting**: Prevent monitoring endpoint abuse
3. **Access Control**: Restrict to authorized users only

Example Spring Security config:
```java
http.authorizeHttpRequests()
    .requestMatchers("/api/monitoring/**").hasRole("ADMIN")
    .anyRequest().authenticated();
```

## Related Configuration

```properties
# Enable HikariCP metrics
spring.datasource.hikari.register-mbeans=true

# Pool configuration
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

## Logging

Monitor logs for connection pool events:

```properties
logging.level.com.zaxxer.hikari=DEBUG
logging.level.com.zaxxer.hikari.HikariConfig=DEBUG
logging.level.com.zaxxer.hikari.pool.HikariPool=DEBUG
```

## Summary

The monitoring endpoints provide real-time visibility into your connection pool:

- 📊 **Metrics**: See exactly how many connections are active/idle
- 🏥 **Health**: Identify issues before they become problems
- 🔍 **Debug**: Troubleshoot connection-related issues
- 📈 **Optimize**: Tune pool size based on actual usage
- 🧪 **Test**: Verify behavior under load

Start monitoring: `curl http://localhost:8080/api/monitoring/hikari/status` 🚀

