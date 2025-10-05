# Tomcat Thread Pool Monitoring Guide

## Overview

This application now includes **Tomcat Thread Pool Monitoring**, allowing you to monitor the HTTP request threads that handle all incoming requests to your application.

## What is the Tomcat Thread Pool?

The Tomcat thread pool manages threads that process HTTP requests. This is different from:
- **HikariCP**: Database connection pool
- **TaskExecutor**: Custom async thread pool (for `@Async` methods)

### Key Difference

| Pool Type | Purpose | Used By |
|-----------|---------|---------|
| **Tomcat Thread Pool** | Handle HTTP requests | ALL endpoints (sync and async) |
| **TaskExecutor Pool** | Execute `@Async` tasks | Only `@Async` methods |
| **HikariCP Pool** | Database connections | Database queries |

**Tomcat threads** are the entry point for every HTTP request. They handle the initial request and either:
1. Process it synchronously (blocking)
2. Hand off to an async executor (non-blocking)

## Features

### 1. REST API Endpoints

Monitor Tomcat threads via JSON APIs:

```bash
# Get current Tomcat metrics
GET http://localhost:8080/api/tomcat/metrics

# Get detailed configuration and status
GET http://localhost:8080/api/tomcat/details

# Get simple status
GET http://localhost:8080/api/tomcat/status

# Stream real-time metrics (SSE)
GET http://localhost:8080/api/tomcat/stream

# Health check
GET http://localhost:8080/api/tomcat/health
```

### 2. Real-time Dashboard

Access the visual dashboard:

```
http://localhost:8080/dashboard/tomcat
```

**Features:**
- Live metrics updates every second
- Thread utilization visualization
- Configuration display
- Automatic alerts for high utilization

## Metrics Explained

### Current Threads
- **Description**: Total number of active threads in the pool
- **Range**: Between `minSpareThreads` and `maxThreads`
- **Normal**: Fluctuates based on load

### Busy Threads
- **Description**: Threads currently processing requests
- **Normal**: < 50% of current threads
- **Warning**: > 70% indicates high load
- **Critical**: > 90% may cause request delays

### Utilization
- **Description**: Percentage of threads in use (Busy / Max * 100)
- **Good**: < 60%
- **Warning**: 60-80%
- **Critical**: > 80%

### Connections
- **Description**: Current active TCP connections
- **Max**: Configured by `server.tomcat.max-connections`
- **Warning**: Near maximum may indicate connection pooling issues

### Completed Tasks
- **Description**: Total requests processed since startup
- **Use**: Measure throughput over time

### Queue Size
- **Description**: Requests waiting for a thread
- **Normal**: 0-5
- **Warning**: > 10 indicates insufficient threads
- **Critical**: > 50 may cause timeouts

## Configuration

Configure Tomcat thread pool in `application.properties`:

```properties
# Tomcat Thread Pool Configuration
server.tomcat.threads.max=200           # Maximum threads
server.tomcat.threads.min-spare=20      # Minimum idle threads
server.tomcat.accept-count=100          # Queue size
server.tomcat.max-connections=8192      # Max TCP connections
server.tomcat.connection-timeout=20000   # Connection timeout (ms)
```

### Recommended Settings

**Low Traffic (Development)**
```properties
server.tomcat.threads.max=50
server.tomcat.threads.min-spare=10
server.tomcat.accept-count=50
```

**Medium Traffic (Production)**
```properties
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=20
server.tomcat.accept-count=100
```

**High Traffic (Heavy Load)**
```properties
server.tomcat.threads.max=400
server.tomcat.threads.min-spare=50
server.tomcat.accept-count=200
```

## How It Works

The monitoring uses **reflection** to access Tomcat's internal thread pool:

```java
// Extract Tomcat WebServer
WebServer webServer = applicationContext.getWebServer();
TomcatWebServer tomcatWebServer = (TomcatWebServer) webServer;

// Access Tomcat internals
org.apache.catalina.startup.Tomcat tomcat = ...;
ThreadPoolExecutor executor = ...;

// Get metrics
int currentThreads = executor.getPoolSize();
int busyThreads = executor.getActiveCount();
int maxThreads = executor.getMaximumPoolSize();
```

## Testing

### 1. Generate Load

Use `ab` (Apache Bench) to generate load:

```bash
# 100 requests, 10 concurrent
ab -n 100 -c 10 http://localhost:8080/api/products/1

# Sustained load
ab -n 10000 -c 50 -t 30 http://localhost:8080/api/products/1
```

### 2. Monitor in Dashboard

Open the dashboard and watch metrics change in real-time:

```
http://localhost:8080/dashboard/tomcat
```

### 3. Compare Sync vs Async

```bash
# Sync endpoint (holds Tomcat thread for entire duration)
ab -n 100 -c 20 http://localhost:8080/api/products/1

# Async endpoint (releases Tomcat thread quickly)
ab -n 100 -c 20 http://localhost:8080/api/products/v2/1
```

**Expected Results:**
- **Sync**: High busy thread count, threads held longer
- **Async**: Lower busy thread count, threads released quickly

## Troubleshooting

### All Metrics Show 0

**Cause**: Reflection failed to access Tomcat internals

**Solution**:
1. Check logs for errors
2. Verify you're using embedded Tomcat (not standalone)
3. Ensure Spring Boot version compatibility

### High Utilization Alerts

**Cause**: Too many concurrent requests for available threads

**Solutions**:
1. Increase `server.tomcat.threads.max`
2. Optimize slow endpoints
3. Use async processing for I/O-bound operations
4. Add caching for frequently accessed data

### Queue Size Growing

**Cause**: Threads can't process requests fast enough

**Solutions**:
1. Increase thread pool size
2. Optimize request processing time
3. Add load balancing
4. Scale horizontally (more instances)

## Comparison: All Three Pools

| Metric | HikariCP | TaskExecutor | Tomcat |
|--------|----------|--------------|--------|
| **Purpose** | Database connections | Async task execution | HTTP request handling |
| **Dashboard** | `/dashboard/hikari` | `/dashboard/threadpool` | `/dashboard/tomcat` |
| **API** | `/api/monitoring/hikari` | `/api/threadpool` | `/api/tomcat` |
| **Default Max** | 10 | 20 | 200 |
| **Used By** | All DB queries | `@Async` methods | All HTTP requests |
| **Bottleneck When** | Long DB transactions | Many async tasks | High request volume |

## Example Scenarios

### Scenario 1: Slow Synchronous Endpoint

```
Endpoint: GET /api/products/{id}
Duration: 3 seconds (DB + external API)
Load: 100 concurrent requests

Tomcat Impact:
- Busy Threads: 100 (or maxThreads)
- Utilization: 50-100%
- Duration: Threads held for full 3 seconds
```

### Scenario 2: Fast Asynchronous Endpoint

```
Endpoint: GET /api/products/v2/{id}
Duration: 3 seconds total (50ms DB + 2.95s external API)
Load: 100 concurrent requests

Tomcat Impact:
- Busy Threads: ~10-20 (brief handoff)
- Utilization: 5-10%
- Duration: Threads held for ~50ms, then released
```

## Best Practices

1. **Monitor All Three Pools**
   - HikariCP: Database connection health
   - TaskExecutor: Async task processing
   - Tomcat: Overall request handling capacity

2. **Use Async for I/O-Bound Operations**
   - External API calls
   - File operations
   - Long-running processes

3. **Set Appropriate Limits**
   - Tomcat threads > TaskExecutor threads
   - TaskExecutor threads ≈ HikariCP connections
   - Adjust based on workload

4. **Watch for Patterns**
   - High Tomcat utilization + Low TaskExecutor = Sync bottleneck
   - High TaskExecutor utilization = Async task backlog
   - High HikariCP utilization = Database bottleneck

## Further Reading

- [Tomcat Configuration Reference](https://tomcat.apache.org/tomcat-9.0-doc/config/http.html)
- [Spring Boot Embedded Containers](https://docs.spring.io/spring-boot/docs/current/reference/html/web.html#web.servlet.embedded-container)
- [Thread Pool Sizing Guide](https://en.wikipedia.org/wiki/Thread_pool)

---

**🎉 You now have complete visibility into all thread pools in your application!**

