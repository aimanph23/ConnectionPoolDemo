# Thread Pool Testing - Complete Summary

## 🎯 Problem Solved!

**Your Question**: "Why is my Thread Pool always showing zero during performance tests?"

**Answer**: You were testing the **wrong endpoint**! 

## Endpoints Comparison

| Endpoint | Thread Type | Uses TaskExecutor? | Shows in Thread Pool Dashboard? |
|----------|-------------|-------------------|--------------------------------|
| `GET /api/products/{id}` | Tomcat HTTP | ❌ NO | ❌ NO |
| `GET /api/products/v2/{id}` | TaskExecutor Async | ✅ YES | ✅ YES |
| `GET /api/customers/async` | TaskExecutor Async | ✅ YES | ✅ YES |
| `GET /api/customers/async/{id}` | TaskExecutor Async | ✅ YES | ✅ YES |

## ✅ What We Added

### 1. Configurable Sleep in Async V2 Endpoint

**Property:**
```properties
# application.properties
product.api.v2.sleep.ms=1000
```

**Location:** Runs in `taskExecutor` thread before returning response

**Code:** `ProductServiceAsync.java` - inside `thenCombine()` lambda

### 2. Test Script

**File:** `test-async-threadpool.sh`

**Usage:**
```bash
./test-async-threadpool.sh
```

### 3. Comprehensive Documentation

- `ASYNC_V2_SLEEP_CONFIG.md` - Detailed guide for async V2 sleep
- `PRODUCT_API_SLEEP_CONFIG.md` - Guide for sync V1 sleep
- `THREAD_POOL_TESTING_SUMMARY.md` - This file!

## 🚀 Quick Start Guide

### Step 1: Configure Sleep
```properties
# application.properties

# Sync endpoint (V1) - uses Tomcat threads
product.api.sleep.ms=1000

# Async endpoint (V2) - uses TaskExecutor threads ✅
product.api.v2.sleep.ms=1000
```

### Step 2: Start Application
```bash
mvn spring-boot:run
```

### Step 3: Open Thread Pool Dashboard
```
http://localhost:8080/dashboard/threadpool
```

### Step 4: Generate Load on ASYNC Endpoint
```bash
# THIS will show activity in Thread Pool Dashboard! ✅
ab -n 1000 -c 50 http://localhost:8080/api/products/v2/1
```

### Step 5: Watch the Magic! ✨

You'll now see:
- ✅ Active Threads: 20 (or more)
- ✅ Pool Size: 20
- ✅ Queue Size: 30+
- ✅ Completed Tasks: Growing counter
- ✅ Utilization: 100%

## 📊 Testing Scenarios

### Scenario 1: Test Async Thread Pool (V2)

**Configuration:**
```properties
product.api.v2.sleep.ms=2000
```

**Test:**
```bash
ab -n 1000 -c 50 http://localhost:8080/api/products/v2/1
```

**Watch:** Thread Pool Dashboard  
**Expect:** High activity, threads busy, queue filling

### Scenario 2: Test Tomcat Threads (V1)

**Configuration:**
```properties
product.api.sleep.ms=2000
```

**Test:**
```bash
ab -n 1000 -c 50 http://localhost:8080/api/products/1
```

**Watch:** Tomcat Dashboard  
**Expect:** Tomcat threads busy, Thread Pool Dashboard shows ZERO

### Scenario 3: Compare Performance

**Test both:**
```bash
# Sync V1
time ab -n 100 -c 10 http://localhost:8080/api/products/1

# Async V2
time ab -n 100 -c 10 http://localhost:8080/api/products/v2/1
```

**Compare:**
- Response times
- Thread utilization
- Connection pool usage

## 🎨 Dashboard URLs

| Dashboard | URL | Shows |
|-----------|-----|-------|
| **Thread Pool** | http://localhost:8080/dashboard/threadpool | TaskExecutor async threads |
| **Tomcat** | http://localhost:8080/dashboard/tomcat | HTTP request threads |
| **HikariCP** | http://localhost:8080/dashboard/hikari | Database connections |
| **Home** | http://localhost:8080/home | All endpoints |

## 📝 Configuration Reference

### Thread Pool Sleep (Async V2)
```properties
# Sleep in taskExecutor thread before returning response
product.api.v2.sleep.ms=1000

# Values:
# 0     = No sleep (default)
# 1000  = 1 second (recommended for testing)
# 3000  = 3 seconds (heavy load testing)
# 5000  = 5 seconds (extreme testing)
```

### Sync Endpoint Sleep (V1)
```properties
# Sleep in Tomcat thread after API call
product.api.sleep.ms=1000
```

### TaskExecutor Configuration
```java
// ConnectionPoolDemoApplication.java
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);      // Initial threads
    executor.setMaxPoolSize(20);       // Max threads
    executor.setQueueCapacity(50);     // Queue size
    executor.setThreadNamePrefix("async-");
    executor.initialize();
    return executor;
}
```

## 🔍 Monitoring Commands

### Real-time Thread Pool Metrics
```bash
watch -n 1 'curl -s http://localhost:8080/api/threadpool/metrics | jq'
```

### Check Thread Names
```bash
curl -s http://localhost:8080/api/threadpool/metrics | jq '.poolName, .type'
```

### Monitor During Load Test
```bash
# Terminal 1: Monitor
while true; do
  curl -s http://localhost:8080/api/threadpool/metrics | \
    jq -r '"Active: \(.activeCount) | Pool: \(.poolSize) | Queue: \(.queueSize)"'
  sleep 1
done

# Terminal 2: Load test
ab -n 5000 -c 100 http://localhost:8080/api/products/v2/1
```

## 🐛 Troubleshooting

### Problem: Thread Pool still shows zero

**Solution:**
1. ✅ Test the **V2** endpoint: `/api/products/v2/1`
2. ✅ NOT the V1 endpoint: `/api/products/1`
3. ✅ Enable sleep: `product.api.v2.sleep.ms=1000`
4. ✅ Generate enough load: `ab -n 1000 -c 50`

### Problem: Pool exhausted too quickly

**Solution:**
```java
// Increase pool size
executor.setMaxPoolSize(50);
executor.setQueueCapacity(100);
```

### Problem: Application hangs

**Solution:**
- Reduce sleep: `product.api.v2.sleep.ms=1000`
- Reduce concurrency: `ab -n 1000 -c 20`

## 📚 Key Learnings

### 1. Thread Types Matter
- **Tomcat threads**: Handle HTTP requests
- **TaskExecutor threads**: Execute `@Async` methods
- **They are separate pools!**

### 2. Endpoint Types
- **Synchronous** (`ResponseEntity<T>`): Uses Tomcat threads
- **Asynchronous** (`CompletableFuture<ResponseEntity<T>>`): Uses TaskExecutor threads

### 3. Monitoring
- Use the **right dashboard** for the **right endpoint**
- Sync endpoints → Tomcat Dashboard
- Async endpoints → Thread Pool Dashboard

### 4. Testing
- Test **async endpoints** to see Thread Pool activity
- Use **configurable sleep** to hold threads longer
- Monitor **all three dashboards** for complete picture

## 🎉 Success Checklist

- [x] Added sleep to async V2 endpoint
- [x] Configured `product.api.v2.sleep.ms` property
- [x] Created test script (`test-async-threadpool.sh`)
- [x] Documented everything
- [x] Explained why Thread Pool was showing zero
- [x] Provided correct endpoints to test

## 🚀 Next Steps

1. **Start the application**
   ```bash
   mvn spring-boot:run
   ```

2. **Run the test script**
   ```bash
   ./test-async-threadpool.sh
   ```

3. **Watch the Thread Pool Dashboard**
   ```
   http://localhost:8080/dashboard/threadpool
   ```

4. **See real activity!** ✨

---

**Now your Thread Pool Dashboard will show real activity when you test the async V2 endpoint!** 🎯

**Key Takeaway:** Always test async endpoints (`/v2/`) to see TaskExecutor thread pool activity!
